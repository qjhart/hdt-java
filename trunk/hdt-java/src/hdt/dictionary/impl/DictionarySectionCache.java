/**
 * Revision: $Rev$
 * Last modified: $Date$
 * Last modified by: $Author$
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contacting the authors:
 *   Mario Arias:               mario.arias@deri.org
 *   Javier D. Fernandez:       jfergar@infor.uva.es
 *   Miguel A. Martinez-Prieto: migumar2@infor.uva.es
 *   Alejandro Andres:          fuzzy.alej@gmail.com
 */
/**
 * 
 */
package hdt.dictionary.impl;

import hdt.dictionary.DictionarySection;
import hdt.listener.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mck
 *
 */
public class DictionarySectionCache implements DictionarySection {
	private DictionarySection child;
	
	private Map<CharSequence,Integer> cacheString;
	private Map<Integer,CharSequence> cacheID;
	final int CACHE_ENTRIES = 128;
	
	/**
	 * 
	 */
	@SuppressWarnings("serial")
	public DictionarySectionCache(DictionarySection child) {
		this.child = child;
		
		// Create cache
		this.cacheString = new LinkedHashMap<CharSequence,Integer>(CACHE_ENTRIES+1, .75F, true) {
		    // This method is called just after a new entry has been added
		    public boolean removeEldestEntry(Map.Entry eldest) {
		        return size() > CACHE_ENTRIES;
		    }
		};

		this.cacheID = new LinkedHashMap<Integer,CharSequence>(CACHE_ENTRIES+1, .75F, true) {
		    // This method is called just after a new entry has been added
		    public boolean removeEldestEntry(Map.Entry eldest) {
		        return size() > CACHE_ENTRIES;
		    }
		};
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public int locate(CharSequence s) {
		Integer o = cacheString.get(s);
		if(o==null) {
			o = child.locate(s);
			cacheString.put(s, o);
		}
 		return o;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(int pos) {
		CharSequence o = cacheID.get(pos);
		if(o==null) {
			o = child.extract(pos);
			cacheID.put(pos, o);
		}
 		return o;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return child.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return child.getNumberOfElements();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<CharSequence> getEntries() {
		return child.getEntries();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener)
			throws IOException {
		child.save(output, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener)
			throws IOException {
		child.load(input, listener);

	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection, hdt.listener.ProgressListener)
	 */
	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		child.load(other, listener);
	}
}
