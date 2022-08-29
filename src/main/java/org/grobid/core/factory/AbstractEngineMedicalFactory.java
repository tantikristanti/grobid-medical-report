package org.grobid.core.factory;

import org.apache.commons.collections4.CollectionUtils;
import org.grobid.core.engines.EngineMedical;
import org.grobid.core.engines.ModelMap;
import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;

import java.util.Collections;
import java.util.Set;

/**
 * 
 * Abstract factory to get engine instance.
 * 
 */
public class AbstractEngineMedicalFactory {

	/**
	 * The engine.
	 */
	private static EngineMedical engine;

	/**
	 * Return a new instance of engine if it doesn't exist, the existing
	 * instance else.
	 * 
	 * @return Engine
	 */
	protected synchronized EngineMedical getEngine() {
		return getEngine(false);
	}

	/**
	 * Return a new instance of engine if it doesn't exist, the existing
	 * instance else.
	 * 
	 * @return Engine
	 */
	protected synchronized EngineMedical getEngine(boolean preload) {
		if (engine == null) {
			engine = createEngine(preload);
		}
		return engine;
	}

	/**
	 * Return a new instance of engine.
	 * 
	 * @return Engine
	 */
	protected EngineMedical createEngine() {
		return createEngine(false);
	}

	/**
	 * Return a new instance of engine.
	 * 
	 * @return Engine
	 */
	protected EngineMedical createEngine(boolean preload) {
		return new EngineMedical(preload);
	}

	/**
	 * Initializes all necessary things for starting grobid
	 */
	public static void init() {
		GrobidProperties.getInstance();
		LibraryLoader.load();
		Lexicon.getInstance();
	}
	
	/**
	 * Initializes all the models 
	 */
	@Deprecated
	public static void fullInit() {
		init();
		Set<GrobidCRFEngine> distinctModels = GrobidProperties.getDistinctModels();
		if (CollectionUtils.containsAny(distinctModels, Collections.singletonList(GrobidCRFEngine.CRFPP))) {  
		    ModelMap.initModels();
        }
		//Lexicon.getInstance();
	}
}
