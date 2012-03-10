/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package de.d3web.we.ci4ke.testing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import de.d3web.we.ci4ke.handling.CIConfig;
import de.d3web.we.ci4ke.testing.CITestResult.Type;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.Article;

/**
 * An abstract implementation of a CITest, which implements the init(CIConfig)
 * and setParameters(List<String>) methods.
 * 
 * @author Marc-Oliver Ochlast (denkbares GmbH)
 * @created 14.11.2010
 */
public abstract class AbstractCITest implements CITest {

	protected CIConfig config;

	protected List<String> parameters;

	public AbstractCITest() {
		this.config = CIConfig.DUMMY_CONFIG;
		this.parameters = new ArrayList<String>();
	}

	@Override
	public void init(CIConfig config) {
		try {
			this.config = (CIConfig) config.clone();
		}
		catch (CloneNotSupportedException e) {
		}
	}

	@Override
	public void setParameters(List<String> parameters) {
		this.parameters = Collections.unmodifiableList(parameters);
	}

	public String getParameter(int index) {
		try {
			return parameters.get(index);
		}
		catch (IndexOutOfBoundsException outOfBoundsExcp) {
			return null;
		}
	}

	/**
	 * Checks if the number of parameters specified for this test are
	 * sufficient.
	 * 
	 * @created 26.11.2010
	 * @param numberOfParameters the required number of parameters for this test
	 * @return false, if this test has less than the required number of
	 *         parameters specified
	 */
	public boolean checkIfParametersAreSufficient(int numberOfParameters) {
		for (int i = 0; i < numberOfParameters; i++) {
			String parameter = getParameter(i);
			if (parameter == null || parameter.trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * When the number of parameters are not sufficient, a test can generate a
	 * appropriate error message with this method.
	 * 
	 * @created 26.11.2010
	 * @param numberOfParametersNeeded the number of parameters this test needs
	 * @return a {@link CITestResult}-Error Message
	 */
	public CITestResult numberOfParametersNotSufficientError(int numberOfParametersNeeded) {
		String errorMessage = "The number of arguments for the test '"
				+ this.getClass().getSimpleName() + "' are not sufficient. Please specify "
				+ numberOfParametersNeeded + " arguments!";
		return new CITestResult(Type.ERROR, errorMessage);
	}

	public Collection<Article> getArticlesMatchingPattern(Pattern pattern) {
		List<Article> matchingArticles = new ArrayList<Article>();
		for (Article article : Environment.getInstance().
				getArticleManager(Environment.DEFAULT_WEB).getArticles()) {
			String articleName = article.getTitle();
			if (pattern.matcher(articleName).matches()) {
				matchingArticles.add(article);
			}
		}
		return Collections.unmodifiableCollection(matchingArticles);
	}
}
