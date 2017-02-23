/*
 * Copyright (C) 2017 denkbares GmbH, Germany
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

package de.knowwe.uitest;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import static junit.framework.TestCase.assertEquals;

/**
 * Edit class text
 *
 * @author Jonas Müller
 * @created 17.02.17
 */
public abstract class BMIUITest extends KnowWEUITest {

	public BMIUITest() {
		super();
	}

	@Override
	public String getTestName() {
		return "Body-Mass-Index";
	}

	@Rule
	public UITestUtils.RetryRule retry = new UITestUtils.RetryRule(2);

	@Test
	public void testBmi() throws Exception {
		changeArticleText(readFile("BMIUITest.txt"));

		By reset = By.className("reset");

		String currentStatus = UITestUtils.getCurrentStatus(getDriver());
		getDriver().findElement(reset).click();
		UITestUtils.awaitStatusChange(getDriver(), currentStatus);

		currentStatus = UITestUtils.getCurrentStatus(getDriver());
		getDriver().findElements(By.className("numinput")).get(0).sendKeys("2" + Keys.ENTER);
		UITestUtils.awaitStatusChange(getDriver(), currentStatus);

		currentStatus = UITestUtils.getCurrentStatus(getDriver());
		List<WebElement> numinput = getDriver().findElements(By.className("numinput"));
		numinput.get(1).sendKeys("100" + Keys.ENTER);
		UITestUtils.awaitStatusChange(getDriver(), currentStatus);

		assertEquals("25", getDriver().findElements(By.className("numinput")).get(2).getAttribute("value"));
		assertEquals("Normal weight", getDriver().findElement(By.className("SOLUTION-ESTABLISHED")).getText());
		assertEquals("bmi = 25", getDriver().findElement(By.className("ABSTRACTION")).getText());
	}

}
