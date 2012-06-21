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
package hdt.triples.impl;

import hdt.enums.TripleComponentOrder;
import hdt.hdt.HDTVocabulary;
import hdt.header.Header;
import hdt.iterator.IteratorTripleID;
import hdt.iterator.SequentialSearchIteratorTripleID;
import hdt.listener.ListenerUtil;
import hdt.listener.ProgressListener;
import hdt.options.ControlInformation;
import hdt.options.HDTSpecification;
import hdt.triples.ModifiableTriples;
import hdt.triples.TripleComparator;
import hdt.triples.TripleID;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Implementation of ModifiableTriples. This is a itable structure.
 * 
 */
public class TriplesList implements ModifiableTriples {

	ControlInformation controlInformation;
	HDTSpecification spec;
	
	/** The array to hold the triples */
	protected ArrayList<TripleID> arrayOfTriples;
	
	/** The order of the triples */
	TripleComponentOrder order;
	int numValidTriples;

	/**
	 * Basic constructor (SPO order)
	 * 
	 */
	public TriplesList() {
		this(new HDTSpecification());
	}

	/**
	 * Constructor, given an order to sort by
	 * 
	 * @param order
	 *            The order to sort by
	 */
	public TriplesList(HDTSpecification specification) {
		super();
		this.controlInformation = new ControlInformation();
		this.arrayOfTriples = new ArrayList<TripleID>();
		this.order = TripleComponentOrder.Unknown;
		this.numValidTriples = 0;
		this.spec = specification;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#search(hdt.triples.TripleID)
	 */
	@Override
	public IteratorTripleID search(TripleID pattern) {
		String patternStr = pattern.getPatternString();
		if(patternStr.equals("???")) {
			return new TriplesListIterator(this);
		} else {
			return new SequentialSearchIteratorTripleID(pattern, new TriplesListIterator(this));
		}
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.Triples#searchAll()
	 */
	@Override
	public IteratorTripleID searchAll() {
		TripleID all = new TripleID(0,0,0);
		return this.search(all);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#cost(hdt.triples.TripleID)
	 */
	@Override
	public float cost(TripleID triple) {
		// TODO Schedule a meeting to discuss how to do this
		return 0;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#getNumberOfElements()
	 */
	@Override
	public long getNumberOfElements() {
		return numValidTriples;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#size()
	 */
	@Override
	public long size() {
		return this.getNumberOfElements()*20;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#save(java.io.OutputStream)
	 */
	@Override
	public void save(OutputStream output, ControlInformation controlInformation, ProgressListener listener) throws IOException {
		controlInformation.clear();
		controlInformation.setInt("numTriples", numValidTriples);
        controlInformation.set("codification", HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
        controlInformation.setInt("triples.component.order", order.ordinal());
        controlInformation.save(output);
		
        DataOutputStream dout = new DataOutputStream(output);
        int count = 0;
		for (TripleID triple : arrayOfTriples) {
			if(triple.isValid()) {
				dout.writeInt(triple.getSubject());
				dout.writeInt(triple.getPredicate());
				dout.writeInt(triple.getObject());
				ListenerUtil.notifyCond(listener, "Saving TriplesList", count, arrayOfTriples.size());
			}
			count++;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#load(java.io.InputStream)
	 */
	@Override
	public void load(InputStream input, ControlInformation controlInformation, ProgressListener listener) throws IOException {
		order = TripleComponentOrder.values()[(int)controlInformation.getInt("triples.component.order")];
		long totalTriples = controlInformation.getInt("numTriples");

		int numRead=0;
		DataInputStream din = new DataInputStream(input);
		
		while(numRead<totalTriples) {
			arrayOfTriples.add(new TripleID(din.readInt(), din.readInt(), din.readInt()));
			numRead++;
			numValidTriples++;
			ListenerUtil.notifyCond(listener, "Loading TriplesList", numRead, totalTriples);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.Triples#load(hdt.triples.ModifiableTriples)
	 */
	@Override
	public void load(ModifiableTriples input, ProgressListener listener) {
		IteratorTripleID iterator = input.searchAll();
		while (iterator.hasNext()) {
			arrayOfTriples.add(iterator.next());
		}
	}

	
	
	
	/**
	 * @param order
	 *            the order to set
	 */
	public void setOrder(TripleComponentOrder order) {
		this.order = order;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#insert(hdt.triples.TripleID[])
	 */
	@Override
	public boolean insert(TripleID... triples) {
		for (TripleID triple : triples) {
			arrayOfTriples.add(new TripleID(triple));
			numValidTriples++;
		}

		return true;
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.ModifiableTriples#insert(int, int, int)
	 */
	@Override
	public boolean insert(int subject, int predicate, int object) {
		arrayOfTriples.add(new TripleID(subject,predicate,object));
		numValidTriples++;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#delete(hdt.triples.TripleID[])
	 */
	@Override
	public boolean remove(TripleID... patterns) {
		boolean removed = false;
		for(TripleID triple : arrayOfTriples){
			for(TripleID pattern : patterns) {
				if(triple.match(pattern)) {
					triple.clear();
					removed = true;
					numValidTriples--;
				}
			}
		}
		return removed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hdt.triples.ModifiableTriples#sort(datatypes.TripleComponentOrder)
	 */
	@Override
	public void sort(TripleComponentOrder order, ProgressListener listener) {
		Collections.sort(arrayOfTriples, new TripleComparator(order));
		this.order = order;
	}
	
	/* (non-Javadoc)
	 * @see hdt.triples.ModifiableTriples#removeDuplicates(hdt.ProgressListener)
	 */
	@Override
	public void removeDuplicates(ProgressListener listener) {
		if(arrayOfTriples.size()<=1) {
			return;
		}
		
		if(order==TripleComponentOrder.Unknown) {
			throw new IllegalArgumentException("Cannot remove duplicates unless sorted");
		}
		
		int j = 0;

        for(int i=1; i<arrayOfTriples.size(); i++) {
                if(arrayOfTriples.get(i).compareTo(arrayOfTriples.get(j))!=0) {
                        j++;
                        arrayOfTriples.set(j, arrayOfTriples.get(i));
                }
                ListenerUtil.notifyCond(listener, "Removing duplicate triples", i, arrayOfTriples.size());
        }

        //cout << "Removed "<< arrayOfTriples.size()-j-1 << " duplicates in " << st << endl;
        while(arrayOfTriples.size()>j+1) {
        	arrayOfTriples.remove(arrayOfTriples.size()-1);
        }
        arrayOfTriples.trimToSize();
        numValidTriples = j+1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TriplesList [" + arrayOfTriples + "\n order=" + order + "]";
	}

	/* (non-Javadoc)
	 * @see hdt.triples.Triples#populateHeader(hdt.header.Header, java.lang.String)
	 */
	@Override
	public void populateHeader(Header header, String rootNode) {
		header.insert(rootNode, HDTVocabulary.TRIPLES_TYPE, HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		header.insert(rootNode, HDTVocabulary.TRIPLES_NUM_TRIPLES, getNumberOfElements() );
		header.insert(rootNode, HDTVocabulary.TRIPLES_ORDER, order.ordinal() );
	}

	/* (non-Javadoc)
	 * @see hdt.triples.ModifiableTriples#replace(int, int, int, int)
	 */
	@Override
	public void replace(int id, int subject, int predicate, int object) {
		arrayOfTriples.get(id).setAll(subject, predicate, object);
	}
	
	public String getType() {
		return HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST;
	}

	@Override
	public void generateIndex(ProgressListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadIndex(InputStream input, ControlInformation ci,
			ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveIndex(OutputStream output, ControlInformation ci,
			ProgressListener listener) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
