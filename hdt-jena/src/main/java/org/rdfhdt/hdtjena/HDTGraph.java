/**
 * File: $HeadURL$
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
 */

package org.rdfhdt.hdtjena;

import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdtjena.solver.HDTJenaIterator;
import org.rdfhdt.hdtjena.solver.OpExecutorHDT;
import org.rdfhdt.hdtjena.solver.ReorderTransformationHDT;

import com.hp.hpl.jena.graph.Capabilities;
import com.hp.hpl.jena.graph.GraphStatisticsHandler;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.engine.main.QC;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * @author mario.arias
 *
 */
public class HDTGraph extends GraphBase {
	private static HDTCapabilities capabilities= new HDTCapabilities();

	private HDT hdt;
	private NodeDictionary nodeDictionary;
	private ReorderTransformation reorderTransform;
	private HDTStatistics hdtStatistics;
	private long numSearches = 0;
	
	static {
		// Register OpExecutor
		QC.setFactory(ARQ.getContext(), OpExecutorHDT.opExecFactoryHDT) ;
	}
	
	public HDTGraph(HDT hdt) {
		this.hdt = hdt;
		this.nodeDictionary = new NodeDictionary(hdt.getDictionary());
		this.hdtStatistics = new HDTStatistics(this);	// Must go after NodeDictionary created.
		this.reorderTransform=new ReorderTransformationHDT(this);  // Must go after Dict and Stats
	}
	
	public HDT getHDT() {
		return hdt;
	}
	
	public NodeDictionary getNodeDictionary() {
		return nodeDictionary;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.TripleMatch)
	 */
	@Override
	protected ExtendedIterator<Triple> graphBaseFind(TripleMatch jenaTriple) {

		TripleID triplePatID = nodeDictionary.getTriplePatID(jenaTriple);
//		System.out.println("Triple Pattern: "+jenaTriple+" as IDs: "+triplePatID);
		
		IteratorTripleID hdtIterator = hdt.getTriples().search( triplePatID );
		numSearches++;
		return new HDTJenaIterator(nodeDictionary, hdtIterator);
	}
	
	public long getNumSearches() {
		return numSearches;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#getStatisticsHandler()
	 */
	@Override
	public GraphStatisticsHandler getStatisticsHandler() {
		return hdtStatistics;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#getCapabilities()
	 */
	@Override
	public Capabilities getCapabilities() {
		return HDTGraph.capabilities;
	}

	public ReorderTransformation getReorderTransform() {
		return reorderTransform;
	}
	
	@Override
	protected int graphBaseSize() {
		return (int)hdt.getTriples().getNumberOfElements();
	}
}