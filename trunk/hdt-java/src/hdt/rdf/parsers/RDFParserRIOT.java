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
package hdt.rdf.parsers;

import hdt.enums.RDFNotation;
import hdt.exceptions.ParserException;
import hdt.rdf.RDFParserCallback;
import hdt.triples.TripleString;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.openjena.atlas.lib.Sink;
import org.openjena.riot.RiotReader;
import org.openjena.riot.lang.LangNTriples;
import org.openjena.riot.lang.LangRDFXML;
import org.openjena.riot.lang.LangTurtle;

import com.hp.hpl.jena.graph.Triple;

/**
 * @author mck
 *
 */
public class RDFParserRIOT implements RDFParserCallback, Sink<Triple> {
	private RDFCallback callback;
	private TripleString triple = new TripleString();
	
	/* (non-Javadoc)
	 * @see hdt.rdf.RDFParserCallback#doParse(java.lang.String, java.lang.String, hdt.enums.RDFNotation, hdt.rdf.RDFParserCallback.Callback)
	 */
	@Override
	public void doParse(String fileName, String baseUri, RDFNotation notation, RDFCallback callback) throws ParserException {
		this.callback = callback;
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(fileName));
			switch(notation) {
				case NTRIPLES:
					LangNTriples langNtriples = RiotReader.createParserNTriples(input,this);
					langNtriples.parse();
					break;
				case RDFXML:
					LangRDFXML langRdfxml = RiotReader.createParserRDFXML(input, baseUri, this);
					langRdfxml.parse();
					break;
				case TURTLE:
					LangTurtle langTurtle = RiotReader.createParserTurtle(input, baseUri, this);
					langTurtle.parse();
					break;
				default:
					throw new ParserException();	
			}
		} catch (FileNotFoundException e) {
			throw new ParserException();
		} catch (Exception e) {
			throw new ParserException();
		}	
	}

	/* (non-Javadoc)
	 * @see org.openjena.atlas.lib.Closeable#close()
	 */
	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.openjena.atlas.lib.Sink#flush()
	 */
	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.openjena.atlas.lib.Sink#send(java.lang.Object)
	 */
	@Override
	public void send(Triple quad) {
		triple.setAll(quad.getSubject().toString(), quad.getPredicate().toString(), quad.getObject().toString());
		callback.processTriple(triple, 0);
	}

}
