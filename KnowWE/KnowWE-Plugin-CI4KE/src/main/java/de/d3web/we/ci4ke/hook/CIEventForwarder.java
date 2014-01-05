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

package de.d3web.we.ci4ke.hook;

import java.util.ArrayList;
import java.util.Collection;

import de.knowwe.core.compile.CompilerFinishedEvent;
import de.knowwe.core.compile.PackageCompiler;
import de.knowwe.core.event.Event;
import de.knowwe.core.event.EventListener;
import de.knowwe.core.kdom.Article;

public class CIEventForwarder implements EventListener {

	@Override
	public Collection<Class<? extends Event>> getEvents() {
		ArrayList<Class<? extends Event>> events = new ArrayList<Class<? extends Event>>(4);
		events.add(CompilerFinishedEvent.class);
		return events;
	}

	@Override
	public void notify(Event event) {
		if (event instanceof CompilerFinishedEvent) {
			de.knowwe.core.compile.Compiler compiler = ((CompilerFinishedEvent<?>) event).getCompiler();
			if (compiler instanceof PackageCompiler) {
				Article article = ((PackageCompiler) compiler).getCompileSection().getArticle();
				CIHookManager.getInstance().triggerHooks(article.getTitle());
			}
		}
	}
}
