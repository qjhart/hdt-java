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

import hdt.compact.array.LogArray64;
import hdt.compact.integer.VByte;
import hdt.dictionary.DictionarySection;
import hdt.exceptions.NotImplementedException;
import hdt.listener.ProgressListener;
import hdt.options.HDTSpecification;
import hdt.util.BitUtil;
import hdt.util.Mutable;
import hdt.util.io.IOUtil;
import hdt.util.string.ByteStringUtil;
import hdt.util.string.ReplazableString;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * @author mck
 *
 */
public class DictionarySectionPFC implements DictionarySection {
	public static final int TYPE_INDEX = 2;
	public static final int DEFAULT_BLOCK_SIZE = 8;
	
	// FIXME: Due to java array indexes being int, only 2GB can be addressed per dictionary section.
	protected byte [] text; // Encoded sequence
	protected int blocksize;
	protected int numstrings;
	protected LogArray64 blocks;
	
	public DictionarySectionPFC(HDTSpecification spec) {
		this.blocksize = (int) spec.getInt("pfc.blocksize");
		if(blocksize==0) {
			blocksize = DEFAULT_BLOCK_SIZE;
		}
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(hdt.dictionary.DictionarySection)
	 */
	@Override
	public void load(DictionarySection other, ProgressListener listener) {
		this.blocks = new LogArray64(BitUtil.log2(other.size()), other.getNumberOfElements()/blocksize);
		this.numstrings = 0;
		//this.text = new byte[(int)other.size()/10];
		
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(byteOut);
		
		CharSequence previousStr=null;
		
		try {
			Iterator<CharSequence> it = other.getEntries();
			while(it.hasNext()) {
				CharSequence str = it.next();
				//ensureSize(bytes+str.length()+4);

				if(numstrings%blocksize==0) {
					// Add new block pointer
					blocks.append(byteOut.size());

					// Copy full string
					ByteStringUtil.append(dout, str, 0);
				} else {
					// Find common part.
					int delta = ByteStringUtil.longestCommonPrefix(previousStr, str);
					// Write Delta in VByte
					VByte.encode(dout, delta);
					// Write remaining
					ByteStringUtil.append(dout, str, delta);
				}
				dout.write(0); // End of string

				numstrings++;
				previousStr = str;
			}

			// Trim text/blocks
			blocks.aggresiveTrimToSize();

			dout.flush();
			text = byteOut.toByteArray();

			// DEBUG
			//dumpAll();
		} catch (IOException e) {

		}
	}
		
	private boolean locateBlock(CharSequence str, Mutable<Integer> block) {
		if(blocks.getNumberOfElements()==0) {
			return false;
		}
		
		int left=0, right=(int)blocks.getNumberOfElements()-1, center=0;
		int cmp=0;
		
		while(left<=right) {
			center = (left+right)/2;
		
			cmp = ByteStringUtil.strcmp(str, text, (int)blocks.get(center));
//			System.out.println("Comparing against block: "+ center + " which is "+ ByteStringUtil.asString(text, (int)blocks.get(center))+ " Result: "+cmp);
			
			if(cmp<0) {
				right = center-1;
			} else {
				if(cmp>0) {
					left = center+1;
				} else {
					block.setValue(center);
					return true;
				}
			}	
		}
		
		if(cmp>0) {
			block.setValue(center);
		} else {
			block.setValue(center-1);
		}
		
		if(block.getValue()<0) {
			block.setValue(0);
		}
		
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#locate(java.lang.CharSequence)
	 */
	@Override
	public int locate(CharSequence str) {
		if(text==null || blocks==null) {
			return 0;
		}
		
		Mutable<Integer> block = new Mutable<Integer>(0);
		boolean cmp = locateBlock(str, block);
		
		if(cmp) {
			return (block.getValue()*blocksize)+1;
		} else {
			int idblock = locateInBlock(block.getValue(), str);
			
			if(idblock != 0) {
				return (block.getValue()*blocksize)+idblock+1;
			}
		}
		
		return 0;
	}
	
	public int locateInBlock(int block, CharSequence str) {
		if(block>=blocks.getNumberOfElements()) {
			return 0;
		}
		
		int pos = (int)blocks.get(block);
		ReplazableString tempString = new ReplazableString();
		
		Mutable<Integer> delta = new Mutable<Integer>(0);
		int idInBlock = 0;
		int cshared=0;
		
//		dumpBlock(block);
		
		// Read the first string in the block
		int slen = ByteStringUtil.strlen(text, pos);
		tempString.append(text, pos, slen);
		pos+=slen+1;
		idInBlock++;
		
		while( (idInBlock<blocksize) && (pos<text.length)) 
		{
			// Decode prefix
			pos += VByte.decode(text, pos, delta);
			
			//Copy suffix
			slen = ByteStringUtil.strlen(text, pos);
			tempString.replace(delta.getValue(), text, pos, slen);
			
			if(delta.getValue()>=cshared)
			{
				// Current delta value means that this string
				// has a larger long common prefix than the previous one
				cshared += ByteStringUtil.longestCommonPrefix(tempString, str, cshared);
				
				if((cshared==str.length()) && (tempString.length()==str.length())) {
					break;
				}
			} else {
				// We have less common characters than before, 
				// this string is bigger that what we are looking for.
				// i.e. Not found.
				idInBlock = 0;
				break;
			}
			pos+=slen+1;
			idInBlock++;
			
		}

		if(pos==text.length || idInBlock== blocksize) {
			idInBlock=0;
		}
		
		return idInBlock;
	}
	
	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#extract(int)
	 */
	@Override
	public CharSequence extract(int id) {
		if(text==null || blocks==null) {
			return null;
		}
		
		if(id<1 || id>numstrings) {
			return null;
		}
		
		int block = (id-1)/blocksize;
		int stringid = (id-1)%blocksize;
		int pos = (int) blocks.get(block);
 		int len = ByteStringUtil.strlen(text, pos);
		
		Mutable<Integer> delta = new Mutable<Integer>(0);
		ReplazableString tempString = new ReplazableString();
		tempString.append(text, pos, len);
		
		for(int i=0;i<stringid;i++) {
			pos+=len+1;
			pos += VByte.decode(text, pos, delta);
			len = ByteStringUtil.strlen(text, pos);
			tempString.replace(delta.getValue(), text, pos, len);
		}
		return tempString;
	}
	
//	private void dumpAll() {
//		for(int i=0;i<blocks.getNumberOfElements();i++) {
//			dumpBlock(i);
//		}
//	}
//	
//	private void dumpBlock(int block) {
//		if(text==null || blocks==null || block>=blocks.getNumberOfElements()) {
//			return;
//		}
//		
//		System.out.println("Dump block "+block);
//		ReplazableString tempString = new ReplazableString();
//		Mutable<Integer> delta = new Mutable<Integer>(0);
//		int idInBlock = 0;
//			
//		int pos = (int)blocks.get(block);
//		
//		// Copy first string
//		int len = ByteStringUtil.strlen(text, pos);
//		tempString.append(text, pos, len);
//		pos+=len+1;
//		
//		System.out.println((block*blocksize+idInBlock)+ " ("+idInBlock+") => "+ tempString);
//		idInBlock++;
//		
//		while( (idInBlock<blocksize) && (pos<text.length)) {
//			pos += VByte.decode(text, pos, delta);
//			
//			len = ByteStringUtil.strlen(text, pos);
//			tempString.replace(delta.getValue(), text, pos, len);
//			
//			System.out.println((block*blocksize+idInBlock)+ " ("+idInBlock+") => "+ tempString + " Delta="+delta.getValue()+ " Len="+len);
//			
//			pos+=len+1;
//			idInBlock++;
//		}
//	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#size()
	 */
	@Override
	public long size() {
		return text.length+blocks.size();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return numstrings;
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#getEntries()
	 */
	@Override
	public Iterator<CharSequence> getEntries() {
		throw new NotImplementedException();
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#save(java.io.OutputStream, hdt.ProgressListener)
	 */
	@Override
	public void save(OutputStream output, ProgressListener listener) throws IOException {
		DataOutputStream dout = new DataOutputStream(output);
		dout.writeByte(TYPE_INDEX);
		dout.writeInt(numstrings);
//		dout.writeInt(maxLength);
		dout.writeLong(text.length);
		IOUtil.writeBuffer(dout, text, 0, text.length, listener);
		dout.writeInt(blocksize);
		blocks.save(dout, listener);
	}

	/* (non-Javadoc)
	 * @see hdt.dictionary.DictionarySection#load(java.io.InputStream, hdt.ProgressListener)
	 */
	@Override
	public void load(InputStream input, ProgressListener listener) throws IOException {
		DataInputStream din = new DataInputStream(input);
		numstrings = din.readInt();
//		maxlength = din.readInt();
		int bytes = (int) din.readLong();
		text = IOUtil.readBuffer(din, bytes, listener);
		blocksize = din.readInt();
		blocks = new LogArray64();
		blocks.load(din, listener);
	}
}
