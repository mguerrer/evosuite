/**
 * Copyright (C) 2011,2012 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * 
 * This file is part of EvoSuite.
 * 
 * EvoSuite is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * EvoSuite is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Public License for more details.
 * 
 * You should have received a copy of the GNU Public License along with
 * EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Gordon Fraser
 */
package org.evosuite.coverage.exception;

import java.util.ArrayList;
import java.util.List;

import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractFitnessFactory;

public class ExceptionCoverageFactory extends AbstractFitnessFactory<TestFitnessFunction> {

	/** {@inheritDoc} */
	@Override
	public List<TestFitnessFunction> getCoverageGoals() {
		return new ArrayList<TestFitnessFunction>();
		//		throw new RuntimeException("Not really sure what this method should do...");
	}

	// TODO: Need to return goals of underlying coverage criterion
	//       plus the exceptions that were observed
	//       -> and then we will need an explicit coverage goal for an exception
}