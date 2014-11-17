package org.sherlok;

import static org.sherlok.mappings.Def.createId;
import static org.sherlok.utils.Create.map;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.sherlok.mappings.BundleDef;
import org.sherlok.mappings.EngineDef;
import org.sherlok.mappings.PipelineDef;
import org.sherlok.mappings.PipelineDef.PipelineEngine;
import org.sherlok.mappings.TypesDef;
import org.sherlok.mappings.TypesDef.TypeDef;
import org.sherlok.utils.ValidationException;
import org.slf4j.Logger;

public class Controller {
    private static final Logger LOG = getLogger(Controller.class);

    private Map<String, TypeDef> typeDefs;
    private Map<String, BundleDef> bundleDefs;
    private Map<String, EngineDef> engineDefs;
    private Map<String, PipelineDef> pipelineDefs;

    public Controller load() throws ValidationException {

        // TYPES
        typeDefs = map(); // reinit
        for (TypesDef tdefs : FileBased.allTypesDefs()) {

            for (TypeDef type : tdefs.getTypes()) {
                String key = type.getShortName();
                if (typeDefs.containsKey(key)) {
                    throw new ValidationException("duplicate types: '" + key
                            + "'");
                } else {
                    typeDefs.put(key, type);
                }
            }
        }

        // BUNDLES
        bundleDefs = map(); // reinit
        for (BundleDef bd : FileBased.allBundleDefs()) {
            bd.validate(bd.toString());
            String key = createId(bd.getName(), bd.getVersion());
            if (bundleDefs.containsKey(key)) {
                throw new ValidationException("duplicate bundle ids: '" + key
                        + "'");
            } else {
                bundleDefs.put(key, bd);
            }
        }

        // ENGINES
        engineDefs = map(); // reinit
        for (EngineDef ed : FileBased.allEngineDefs()) {
            ed.validate(ed.toString());

            // bundle must be found
            if (!bundleDefs.containsKey(ed.getBundleId())) {
                throw new ValidationException(
                        "no bundle def found for engine '" + ed + "'");
            }
            // no duplicate engine ids
            if (engineDefs.containsKey(ed.getId())) {
                throw new ValidationException("duplicate engine ids: '" + ed
                        + "'");
            } else {
                engineDefs.put(ed.getId(), ed);
            }
        }

        // PIPELINES
        pipelineDefs = map(); // reinit
        for (PipelineDef pd : FileBased.allPipelineDefs()) {
            pd.validate(pd.toString());

            // all engines must be found
            for (PipelineEngine pe : pd.getEngines()) {
                if (pe.getId() != null && !engineDefs.containsKey(pe.getId())) {
                    throw new ValidationException("no engine def found for '"
                            + pe + "' in pipeline '" + pe + "'");
                }
            }
            // no duplicate pipeline ids
            if (pipelineDefs.containsKey(pd.getId())) {
                throw new ValidationException("duplicate pipeline id '"
                        + pd.getId() + "'");
            } else {
                pipelineDefs.put(pd.getId(), pd);
            }
        }

        LOG.debug(
                "Done loading from Store: {} typeDefs, {} bundleDefs, {} engineDefs, {} pipelineDefs",
                new Object[] { typeDefs.size(), bundleDefs.size(),
                        engineDefs.size(), pipelineDefs.size() });
        return this;
    }

    // /////////////////////////////////////////////////////////////////////
    // access API, package visibility

    // LIST all /////////////////////////////////////////////////////////////
    Collection<TypeDef> listTypes() {
        return typeDefs.values();
    }

    Collection<BundleDef> listBundles() {
        return bundleDefs.values();
    }

    Collection<EngineDef> listEngines() {
        return engineDefs.values();
    }

    Collection<PipelineDef> listPipelines() {
        return pipelineDefs.values();
    }

    // LIST all names /////////////////////////////////////////////////////////
    Set<String> listBundleDefNames() {
        return bundleDefs.keySet();
    }

    Set<String> listTypeDefNames() {
        return typeDefs.keySet();
    }

    Set<String> listEngineDefNames() {
        return engineDefs.keySet();
    }

    Set<String> listPipelineDefNames() {
        return pipelineDefs.keySet();
    }

    // GET by name /////////////////////////////////////////////////////////
    TypeDef getTypeDef(String typeShortName) {
        return typeDefs.get(typeShortName);
    }

    BundleDef getBundleDef(String bundleId) {
        return bundleDefs.get(bundleId);
    }

    EngineDef getEngineDef(String engineId) {
        return engineDefs.get(engineId);
    }

    PipelineDef getPipelineDef(String pipelineId) {
        return pipelineDefs.get(pipelineId);
    }

    // PUT /////////////////////////////////////////////////////////////
    String putBundle(String bundleStr) throws ValidationException {
        BundleDef newBundle = FileBased.putBundle(bundleStr);
        bundleDefs.put(newBundle.getId(), newBundle);
        return newBundle.getId();
    }

    String putEngine(String engineStr) throws ValidationException {
        EngineDef newEngine = FileBased.putEngine(engineStr);
        engineDefs.put(newEngine.getId(), newEngine);
        return newEngine.getId();
    }

    String putPipeline(String pipelineStr) throws ValidationException {
        PipelineDef newPipeline = FileBased.putPipeline(pipelineStr);
        pipelineDefs.put(newPipeline.getId(), newPipeline);
        return newPipeline.getId();
    }

    // DELETE /////////////////////////////////////////////////////////////
    void deleteBundleDef(String bundleId) throws ValidationException {
        if (!bundleDefs.containsKey(bundleId)) {
            throw new ValidationException("bundle '" + bundleId + "' not found");
        }
        FileBased.deleteBundle(bundleId);
        bundleDefs.remove(bundleId);
    }

    void deleteEngineDef(String engineId) throws ValidationException {
        if (!engineDefs.containsKey(engineId)) {
            throw new ValidationException("engine '" + engineId + "' not found");
        }
        String domain = engineDefs.get(engineId).getDomain();
        FileBased.deleteEngine(engineId, domain);
        engineDefs.remove(engineId);
    }

    void deletePipelineDef(String pipelineId) throws ValidationException {
        if (!pipelineDefs.containsKey(pipelineId)) {
            throw new ValidationException("pipeline '" + pipelineId
                    + "' not found");
        }
        String domain = pipelineDefs.get(pipelineId).getDomain();
        FileBased.deletePipeline(pipelineId, domain);
        pipelineDefs.remove(pipelineId);
    }
}