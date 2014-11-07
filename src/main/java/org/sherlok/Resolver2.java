package org.sherlok;

import static ch.epfl.bbp.collections.Create.list;
import static ch.epfl.bbp.collections.Create.map;
import static ch.epfl.bbp.collections.Create.set;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.sherlok.mappings.BundleDef;
import org.sherlok.mappings.EngineDef;
import org.sherlok.mappings.MavenPom;
import org.sherlok.mappings.PipelineDef;
import org.sherlok.mappings.PipelineDef.PipelineEngine;
import org.sherlok.mappings.Store;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

import sherlok.aether.Booter;
import sherlok.aether.ConsoleDependencyGraphDumper;
import freemarker.template.TemplateException;

/**
 * Resolves the transitive dependencies of an artifact.
 */
public class Resolver2 {

    private Store store;

    public Resolver2() {
        this.store = new Store().load();
    }

    private void solveDeps(String fakePom, Map<String, String> repositoriesDefs)
            throws ArtifactResolutionException, DependencyCollectionException,
            IOException {

        RepositorySystem system = Booter.newRepositorySystem();
        RepositorySystemSession session = Booter
                .newRepositorySystemSession(system);

        Artifact rootArtifact = new DefaultArtifact(fakePom);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(rootArtifact, ""));
        collectRequest.setRepositories(Booter.newRepositories(system, session,
                new HashMap<String, String>()));

        CollectResult collectResult = system.collectDependencies(session,
                collectRequest);

        collectResult.getRoot().accept(new ConsoleDependencyGraphDumper());

        PreorderNodeListGenerator p = new PreorderNodeListGenerator();
        collectResult.getRoot().accept(p);
        for (Dependency dependency : p.getDependencies(true)) {

            Artifact artifact = dependency.getArtifact();
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(artifact);
            artifactRequest.setRepositories(Booter.newRepositories(system,
                    session, repositoriesDefs));

            ArtifactResult artifactResult = system.resolveArtifact(session,
                    artifactRequest);
            // System.out.println("RESOLVED:: " + artifactResult.isResolved());

            artifact = artifactResult.getArtifact();

            ClassPathHack.addFile(artifact.getFile());

            // System.out
            // .println("FILE:: " + artifact.getFile().getAbsolutePath());
        }
    }

    @SuppressWarnings("unchecked")
    static AnalysisEngineDescription createEngine(String cName,
            Object... params) throws ClassNotFoundException,
            ResourceInitializationException {

        // instantiate class
        Class<? extends AnalysisComponent> classz = (Class<? extends AnalysisComponent>) Class
                .forName(cName);
        // create ae
        AnalysisEngineDescription aed = AnalysisEngineFactory
                .createEngineDescription(classz, params);

        return aed;
    }

    private Map<String, Pipeline> pipelineCache = map();

    public Pipeline resolve(String pipelineName, String version)
            throws UIMAException, ArtifactResolutionException,
            DependencyCollectionException, IOException, ClassNotFoundException,
            SAXException, CpeDescriptorException, TemplateException {

        // 1. get pipeline from cache of components
        String pipelineId = PipelineDef.createId(pipelineName, version);
        if (pipelineCache.containsKey(pipelineId)) {
            return pipelineCache.get(pipelineId);

        } else {
            // 2. else, load it
            // 2.1 read pipeline def
            PipelineDef pipelineDef = store.getPipelineDef(pipelineId);
            checkNotNull(pipelineDef, "could not find " + pipelineId);

            // 2.2 resolve engines
            // "de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter"));
            // "de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger"));
            // "de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder",
            // "modelVariant", "person"));
            // "de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpNameFinder",
            // "modelVariant", "location"));

            // 2.3 resolve engines (and their bundles)
            List<EngineDef> engineDefs = list();
            Set<BundleDef> bundleDefs = set();
            Map<String, String> repositoriesDefs = map();
            for (PipelineEngine pengine : pipelineDef.getEngines()) {
                EngineDef engineDef = store.getEngineDef(pengine.getId());
                checkNotNull(engineDef, "could not find " + pengine);
                engineDefs.add(engineDef);
                BundleDef bundleDef = store.getBundleDef(engineDef
                        .getBundleId());
                checkNotNull(bundleDef, "could not find " + bundleDef);
                bundleDefs.add(bundleDef);
                for (Entry<String, String> id_url : bundleDef.getRepositories()
                        .entrySet()) {
                    repositoriesDefs.put(id_url.getKey(), id_url.getValue());
                }
            }

            // 2.4 create fake POM from bundles and copy it
            String fakePom = MavenPom.writePom(bundleDefs, pipelineName,
                    version);

            // 2.4 solve dependecies
            solveDeps(fakePom, repositoriesDefs);

            // 3 create pipeline and add components
            Pipeline pipeline = new Pipeline(pipelineId);
            for (EngineDef engineDef : engineDefs) {
                pipeline.add(createEngine(engineDef.getClassz(),
                        engineDef.getFlatParams()));
            }

            // 3.2 set annotations to output
            pipeline.addOutputAnnotation(
                    "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity",
                    "value");
            // FIXME

            // 3.3 initialize pipeline and cache it
            pipeline.initialize();
            pipelineCache.put(pipelineId, pipeline);

            return pipeline;
        }
    }

    /** reflection to bypass encapsulation */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static class ClassPathHack {
        private static final Class[] parameters = new Class[] { URL.class };

        public static void addFile(String s) throws IOException {
            File f = new File(s);
            addFile(f);
        }

        public static void addFile(File f) throws IOException {
            addURL(f.toURI().toURL());
        }

        public static void addURL(URL u) throws IOException {
            URLClassLoader sysloader = (URLClassLoader) ClassLoader
                    .getSystemClassLoader();
            Class sysclass = URLClassLoader.class;

            try {
                Method method = sysclass
                        .getDeclaredMethod("addURL", parameters);
                method.setAccessible(true);
                method.invoke(sysloader, new Object[] { u });
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IOException(
                        "Error, could not add URL to system classloader");
            }
        }
    }
}