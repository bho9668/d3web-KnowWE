/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package de.d3web.we.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchWordPreprocessor {
	
	private static SearchWordPreprocessor instance;
	
	public static SearchWordPreprocessor getInstance() {
		if (instance == null) {
			instance = new SearchWordPreprocessor();
			
		}

		return instance;
	}
	
	private static List<SearchWordPreprocessingModule> processors = new ArrayList<SearchWordPreprocessingModule>(); 
	
	public Collection<SearchTerm> process(String searchText) {
		String [] words = searchText.split(" ");
		
		Map<String,SearchTerm> result = new HashMap<String,SearchTerm>();
		
		for (String word : words) {
			result.put(word, new SearchTerm(word));
		}
		
		for (SearchWordPreprocessingModule module : processors) {
			Collection<SearchTerm> list = module.getTerms(searchText);
			for (SearchTerm searchTerm : list) {
				SearchTerm t = result.get(searchTerm.getTerm());
				if(t != null) {
					t.setImportance(t.getImportance() + searchTerm.getImportance());
				}else {
					result.put(searchTerm.getTerm(), searchTerm);
				}
			}
			
		}
		
		
		return result.values();
		
	}

}
