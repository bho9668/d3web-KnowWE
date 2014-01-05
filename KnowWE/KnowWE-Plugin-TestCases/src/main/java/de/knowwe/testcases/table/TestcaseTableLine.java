/*
 * Copyright (C) 2010 University Wuerzburg, Computer Science VI
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
package de.knowwe.testcases.table;

import de.knowwe.core.report.Message;
import de.knowwe.core.report.MessageRenderer;
import de.knowwe.kdom.table.TableCell;
import de.knowwe.kdom.table.TableLine;

/**
 * 
 * @author Sebastian Furth
 * @created 20/10/2010
 */
public class TestcaseTableLine extends TableLine {

	public static final String TESTCASE_KEY = "TESTCASE";

	public TestcaseTableLine() {
		setRenderer(new TestcaseTableLineRenderer());
		replaceChildType(new Cell(), TableCell.class);
		addCompileScript(new TestcaseTableLineSubtreeHandler());
	}

	@Override
	public MessageRenderer getMessageRenderer(Message.Type messageType) {
		return null;
	}
}
