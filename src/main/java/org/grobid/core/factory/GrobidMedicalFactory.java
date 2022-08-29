package org.grobid.core.factory;

import org.grobid.core.engines.EngineMedical;

/**
 *
 * Factory to get engine instances.
 *
 */
public class GrobidMedicalFactory extends GrobidFactory {

    private static EngineMedical engine;

    /**
     * The instance of GrobidMedicalFactory.
     */
    private static GrobidMedicalFactory factory = null;


    /**
     * Constructor.
     */
    protected GrobidMedicalFactory() {
        init();
    }

    /**
     * Return a new instance of GrobidMedicalFactory if it doesn't exist, the existing
     * instance else.
     *
     * @return GrobidFactory
     */
    public static GrobidMedicalFactory getInstance() {
        if (factory == null) {
            factory = newInstance();
        }
        return factory;
    }

    public EngineMedical getEngine() {
        return getEngine(false);
    }

    public  EngineMedical getEngine(boolean preload) {
        if (engine == null) {
            engine = createEngine(preload);
        }
        return engine;
    }

    public EngineMedical createEngine() {
        return createEngine(false);
    }

    /**
     * Return a new instance of engine.
     *
     * @return EngineMedical
     */
    public EngineMedical createEngine(boolean preload) {
        return new EngineMedical(preload);
    }

    /**
     * Creates a new instance of GrobidMedicalFactory.
     *
     * @return GrobidMedicalFactory
     */
    protected static GrobidMedicalFactory newInstance() {
        return new GrobidMedicalFactory();
    }

    /**
     * Resets this class and all its static fields. For instance sets the
     * current object to null.
     */
    public static void reset() {
        factory = null;
    }

}
