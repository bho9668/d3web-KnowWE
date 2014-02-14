/*
 * Copyright (C) 2014 denkbares GmbH
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
package de.knowwe.include.export;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.include.export.DocumentBuilder.Style;
import de.knowwe.jspwiki.types.DefinitionType;
import de.knowwe.jspwiki.types.DefinitionType.DefinitionData;
import de.knowwe.jspwiki.types.DefinitionType.DefinitionHead;

/**
 * Class to export definitions as unordered list with bold definition text.
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 07.02.2014
 */
public class DefinitionExporter implements Exporter<DefinitionType> {

	@Override
	public boolean canExport(Section<DefinitionType> section) {
		return true;
	}

	@Override
	public Class<DefinitionType> getSectionType() {
		return DefinitionType.class;
	}

	@Override
	public void export(Section<DefinitionType> section, DocumentBuilder manager) {
		Section<DefinitionHead> head = Sections.successor(section, DefinitionHead.class);
		Section<DefinitionData> data = Sections.successor(section, DefinitionData.class);

		BigInteger abstractID = ListExporter.getAbstractIdUnordered();
		BigInteger numID = manager.getDocument().getNumbering().addNum(abstractID);
		XWPFParagraph paragraph = manager.getNewParagraph(Style.list);
		paragraph.setNumID(numID);
		paragraph.getCTP().getPPr().getNumPr().addNewIlvl().setVal(BigInteger.valueOf(0));

		manager.setBold(true);
		manager.append(head.getText());
		manager.append(": ");
		manager.getParagraph().createRun().addCarriageReturn();

		manager.setBold(false);
		manager.append(data.getText());
		manager.closeParagraph();
	}
}