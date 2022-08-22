package org.grobid.core.factory;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.grobid.core.engines.EngineMedical;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class GrobidMedicalPoolingFactory extends AbstractEngineMedicalFactory implements
    PoolableObjectFactory<EngineMedical> {

	/**
        * A pool which contains objects of type EngineMedical for the conversion.
        */
    private static volatile GenericObjectPool<EngineMedical> grobidEnginePool = null;
    private static volatile Boolean grobidEnginePoolControl = false;
    private static final Logger LOGGER = LoggerFactory
        .getLogger(GrobidPoolingFactory.class);

    private static volatile Boolean preload = false;

    /**
     * Constructor.
     */
    protected GrobidMedicalPoolingFactory() {
        init();
    }

    /**
     * Creates a pool for {@link EngineMedical} objects. So a number of objects is
     * always available and ready to start immediatly.
     *
     * @return GenericObjectPool
     */
    protected static GenericObjectPool<EngineMedical> newPoolInstance() {
        if (grobidEnginePool == null) {
            LOGGER.debug("Synchronized new pool instance");
            synchronized (grobidEnginePoolControl) {
                if (grobidEnginePool == null) {
                    grobidEnginePool = new GenericObjectPool<>(GrobidMedicalPoolingFactory.newInstance());
                    grobidEnginePool
                        .setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
                    grobidEnginePool.setMaxWait(GrobidProperties.getPoolMaxWait());
                    grobidEnginePool.setMaxActive(GrobidProperties.getMaxConcurrency());
                    grobidEnginePool.setTestWhileIdle(false);
                    grobidEnginePool.setLifo(false);
                    grobidEnginePool.setTimeBetweenEvictionRunsMillis(2000);
                    grobidEnginePool.setMaxIdle(0);
                }
            }
        }
        return grobidEnginePool;
    }

    /**
     * Obtains an instance from this pool.<br>
     *
     * By contract, clients must call {@link GrobidMedicalPoolingFactory#returnEngine}
     * when they finish to use the engine.
     */
    public static synchronized EngineMedical getEngineFromPool(boolean preloadModels) {
        preload = preloadModels;
        if (grobidEnginePool == null) {
            grobidEnginePool = newPoolInstance();
        }
        EngineMedical engineMedical = null;
        try {
            engineMedical = grobidEnginePool.borrowObject();
        } catch (NoSuchElementException nseExp) {
            throw new NoSuchElementException();
        } catch (Exception exp) {
            throw new GrobidException("An error occurred while getting an engine from the engine pool", exp);
        }
        LOGGER.info("Number of Engines in pool active/max: "
            + grobidEnginePool.getNumActive() + "/"
            + grobidEnginePool.getMaxActive());
        return engineMedical;
    }

    /**
     * By contract, engine must have been obtained using
     * {@link GrobidPoolingFactory#getEngineFromPool}.<br>
     */
    public static void returnEngine(EngineMedical engine) {
        try {
            //engine.close();
            if (grobidEnginePool == null)
                LOGGER.error("grobidEnginePool is null !");
            grobidEnginePool.returnObject(engine);
        } catch (Exception exp) {
            throw new GrobidException(
                "An error occurred while returning an engine from the engine pool", exp);
        }
    }

    /**
     * Creates and returns an instance of GROBIDFactory. The init() method will
     * be called.
     *
     * @return
     */
    protected static GrobidMedicalPoolingFactory newInstance() {
        return new GrobidMedicalPoolingFactory();
    }

    @Override
    public void activateObject(EngineMedical arg0) throws Exception {
    }

    @Override
    public void destroyObject(EngineMedical engine) throws Exception {
    }


    @Override
    public EngineMedical makeObject() throws Exception {
        return (createEngine(this.preload));
    }

    @Override
    public void passivateObject(EngineMedical arg0) throws Exception {
    }

    @Override
    public boolean validateObject(EngineMedical arg0) {
        return false;
    }

}
