/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.core.explanations.preprocessors.impl.mus;

import org.prop4j.solver.AbstractSolverFactory;
import org.prop4j.solver.IMusExtractor;
import org.prop4j.solver.impl.SatProblem;
import org.prop4j.solver.impl.SolverManager;
import org.prop4j.solvers.impl.javasmt.sat.JavaSmtSatMusExtractor;

import de.ovgu.featureide.fm.core.explanations.preprocessors.PreprocessorExplanation;
import de.ovgu.featureide.fm.core.explanations.preprocessors.PreprocessorExplanationCreator;
import de.ovgu.featureide.fm.core.explanations.preprocessors.impl.AbstractPreprocessorExplanationCreator;

/**
 * Abstract implementation of {@link PreprocessorExplanationCreator} using a {@link IMusExtractor MUS extractor}.
 *
 * @param <S> subject
 * @param <E> explanation
 * @author Timo G&uuml;nther
 */
public abstract class MusPreprocessorExplanationCreator<S, E extends PreprocessorExplanation<S>>
		extends AbstractPreprocessorExplanationCreator<S, E, IMusExtractor> {

	/** The solver factory used to create the oracle. */
	private final AbstractSolverFactory solverFactory;

	/**
	 * Constructs a new instance of this class.
	 *
	 * @param solverFactory the solver factory used to create the oracle
	 */
	protected MusPreprocessorExplanationCreator(AbstractSolverFactory solverFactory) {
		if (solverFactory == null) {
			solverFactory = AbstractSolverFactory.getDefault();
		}
		this.solverFactory = solverFactory;
	}

	/**
	 * Returns the solver factory used to create the oracle
	 *
	 * @return the solver factory
	 */
	public AbstractSolverFactory getSatSolverFactory() {
		return solverFactory;
	}

	@Override
	protected IMusExtractor createOracle() {
		final IMusExtractor oracle = SolverManager.getSelectedFeatureModelDefectExplanatorSolverFactory().getExplanationSolver(new SatProblem(getCnf()));
		if (oracle instanceof JavaSmtSatMusExtractor) {
			final JavaSmtSatMusExtractor sol = (JavaSmtSatMusExtractor) oracle;
//			FMCorePlugin.getDefault().logInfo("created : (" + sol.context.getClass().getName() + ") " + oracle.getClass().getName());
		} else {
//			FMCorePlugin.getDefault().logInfo("created : " + oracle.getClass().getName());
		}
		return oracle;
	}
}
