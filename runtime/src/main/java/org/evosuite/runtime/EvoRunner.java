/**
 * Copyright (C) 2010-2015 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser Public License as published by the
 * Free Software Foundation, either version 3.0 of the License, or (at your
 * option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along
 * with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.evosuite.annotations.EvoSuiteTest;
import org.junit.Test;
import org.evosuite.runtime.instrumentation.EvoClassLoader;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special JUnit Runner needed for the test cases generated by EvoSuite
 *
 * @author arcuri
 *
 */
public class EvoRunner extends BlockJUnit4ClassRunner {

	/*
	 * We need this class due to some weird behavior of JVM
	 */

    private static final Logger logger = LoggerFactory.getLogger(EvoRunner.class);

    /**
     * Dirty hack, to use with care.
     * In some very special cases, we want to skip agent instrumentation.
     * Still, we would need to use a classloader that will do such instrumentation.
     * This is for example done in -measureCoverage, as we need a more details instrumentation,
     * and, at the same time, we want to avoid a double instrumentation from agent
     */
    public static boolean useAgent = true;


    public EvoRunner(Class<?> klass)
            throws InitializationError {
		/*
		 * extremely important that getClass is called _BEFORE_ super is executed.
		 * The constructor of BlockJUnit4ClassRunner does reflection on klass, eg
		 * to check that it has only one constructor.
		 * For some arcane reasons, such reflection code ends up in native JVM code
		 * that "might" start the loading of some classes whose type is used for
		 * variables and casting inside the methods of "klass", 
		 * although the code of those methods is _NOT_ executed (note: not
		 * talking of static initializers here).
		 */
        super(getClass(klass));
    }

    private static Class<?> getClass(Class<?> klass) throws InitializationError{

        EvoRunnerParameters ep = klass.getAnnotation(EvoRunnerParameters.class);

        if(ep == null){
            throw new IllegalStateException("EvoSuite test class "+klass.getName()+
                    " is not annotated with "+EvoRunnerParameters.class.getName());
        }

        RuntimeSettings.resetStaticState = ep.resetStaticState();
        RuntimeSettings.mockJVMNonDeterminism = ep.mockJVMNonDeterminism();
        RuntimeSettings.useVFS = ep.useVFS();
        RuntimeSettings.useVNET = ep.useVNET();
        RuntimeSettings.useSeparateClassLoader = ep.separateClassLoader();
        RuntimeSettings.useJEE = ep.useJEE();

        if(RuntimeSettings.useSeparateClassLoader) {
            return getFromEvoSuiteClassloader(klass);
        }

        if(useAgent) {
            org.evosuite.runtime.agent.InstrumentingAgent.initialize();
        }

        org.evosuite.runtime.agent.InstrumentingAgent.activate();

        try {
			/*
			 *  be sure that reflection on "klass" is executed here when
			 *  the agent is active
			 */
            klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            //shouldn't really happen
            logger.error("Failed to initialize test class "+klass.getName());
        }
        org.evosuite.runtime.agent.InstrumentingAgent.deactivate();

        return klass;
    }

    private static Class<?> getFromEvoSuiteClassloader(Class<?> clazz) throws InitializationError {
        try {
	    	/*
	    	 *  properties like REPLACE_CALLS will be set directly in the JUnit files
	    	 */

            // LoggingUtils.loadLogbackForEvoSuite();

	    	/*
	    	 * TODO: this approach does throw away all the possible instrumentation done on the input clazz,
	    	 * eg code coverage of Emma, Cobertura, Javalanche, etc.
	    	 * 
	    	 * maybe an option would be to use java agents:
	    	 * 
	    	 * http://dhruba.name/2010/02/07/creation-dynamic-loading-and-instrumentation-with-javaagents/
	    	 * http://www.eclemma.org/jacoco/trunk/doc/implementation.html
	    	 * http://osi.fotap.org/2008/06/27/dynamically-installing-agents-in-java-6/
	    	 * http://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html
	    	 */

            EvoClassLoader classLoader = new EvoClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            return Class.forName(clazz.getName(), true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new InitializationError(e);
        }
    }

    /**
     * Returns the methods that run tests. Default implementation returns all
     * methods annotated with {@code @Test} on this class and superclasses that
     * are not overridden.
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        Set<FrameworkMethod> testMethods = new HashSet<FrameworkMethod>();
        testMethods.addAll(getTestClass().getAnnotatedMethods(EvoSuiteTest.class));
        testMethods.addAll(getTestClass().getAnnotatedMethods(Test.class));
        return new ArrayList<FrameworkMethod>(testMethods);
    }

    /**
     * Adds to {@code errors} for each method annotated with {@code @Test}that
     * is not a public, void instance method with no arguments.
     */
    @Override
    protected void validateTestMethods(List<Throwable> errors) {
        Set<FrameworkMethod> testMethods = new HashSet<FrameworkMethod>();
        testMethods.addAll(getTestClass().getAnnotatedMethods(EvoSuiteTest.class));
        testMethods.addAll(getTestClass().getAnnotatedMethods(Test.class));
        for (FrameworkMethod eachTestMethod : testMethods) {
            eachTestMethod.validatePublicVoidNoArg(false, errors);
        }
    }

}
