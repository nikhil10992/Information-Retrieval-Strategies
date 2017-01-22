package LuceneExplorer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class LuceneMain {
	
	static List<String> outputList = new ArrayList<String>();
	
	public static void main(String[] args) {
		List<List<String>> qTermList = new ArrayList<List<String>>();
		List<String> tempQList;
		
		Hashtable<String, LinkedList<Integer>> termPostingsList = new Hashtable<String, LinkedList<Integer>>();
		
		Path indexPath;
		BufferedReader bInput = null;
		BufferedWriter bOutput = null;
		String line, pListing = "";
		String indexPathString = null;
		File outputFile = null, inputFile = null;
		int pListSize = 0;
		
		try {
			 	indexPathString = args[0];
			 	outputFile = new File(args[1]);
			 	inputFile = new File(args[2]);
	    }
	    catch (ArrayIndexOutOfBoundsException e){
	        System.out.println("ArrayIndexOutOfBoundsException caught");
	    }
		
		try{
			bInput = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
			bOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));	
			while ((line = bInput.readLine()) != null)
			{
				tempQList = new ArrayList<String>(); 
				for(String qTerm : line.split(" "))
					tempQList.add(qTerm);
				qTermList.add(tempQList);
			}
			bInput.close();
			
			indexPath = FileSystems.getDefault().getPath(indexPathString);
			termPostingsList = createPostingsList(indexPath);				
			for (List<String> termList : qTermList)
			{
				for(String term : termList)
				{
					outputList.add("GetPostings");
					outputList.add(term);
					pListing = "";
					try{
						pListSize = termPostingsList.get(term).size();
						for (int i = 0; i < pListSize ; i++)
							pListing = pListing.concat(termPostingsList.get(term).get(i)  + " ");
						outputList.add("Postings list: " + pListing);
					}catch (NullPointerException e)
					{
						outputList.add("Postings list: " + 0);
					}
				}
				TaatAnd(termPostingsList, termList);
				TaatOr(termPostingsList, termList);
				DaatAnd(termPostingsList, termList);
				DaatOr(termPostingsList, termList);
			}
			for (String item : outputList)
			{
				bOutput.write(item);
				bOutput.newLine();
			}
			bOutput.close();
		}
		catch(IOException IO)
		{
			IO.printStackTrace();
		}
		System.out.println("Done");
	}
	
	public static void DaatOr(Hashtable<String, LinkedList<Integer>> termPostingsList, List<String> queryTerms)
	{
		ArrayList<LinkedList<Integer>> qTPList = new ArrayList<LinkedList<Integer>>();
		
		LinkedList<Integer> finalPLL = new LinkedList<Integer>();
		LinkedList<Integer> tempLL;
		LinkedList<Integer> LargestPLL = new LinkedList<Integer>();
		
		int maxPLen = 0, pLen = 0, maxValue = Integer.MIN_VALUE, otherValue = 0;
		int numOFPList = 0, comparisons = 0, lengthSPList = 0, numOfQterms = 0;
		String qTerm = "";
		List<Integer> startIndex = new ArrayList<Integer>();
		boolean found = false;
		
		// Get the smallest postings list.
		numOfQterms = queryTerms.size();
		for (int s = 0; s < numOfQterms; s++)
		{
			qTerm = queryTerms.get(s);
			tempLL = new LinkedList<Integer>();
			tempLL = (LinkedList<Integer>)deepClone(termPostingsList.get(qTerm));
			if (tempLL == null)
				continue;
			pLen = tempLL.size();
			if (maxPLen < pLen)
			{
				maxPLen = pLen;
				LargestPLL = tempLL;
			}	
			qTPList.add(tempLL);
		}
		qTPList.remove(LargestPLL);
		
		numOFPList = qTPList.size();
		lengthSPList = LargestPLL.size();
		
		for (int t = 0; t < numOFPList; t++)
			startIndex.add(0);
		for (int i = 0; i < lengthSPList; i++)
		{
			maxValue = (int)LargestPLL.get(i);
			int m = 0;
			found = false;
			for (int j = 0; j < numOFPList; j++)
			{
				m = startIndex.get(j);
				while (m <= (qTPList.get(j).size() - 1))
				{
					comparisons++;
					otherValue = (int)qTPList.get(j).get(m);
					if (i == lengthSPList - 1)
					{
						if(LargestPLL.indexOf(otherValue) == -1)
						{
							LargestPLL.add(otherValue);
							lengthSPList++;
						}
					}
					else if (maxValue > otherValue)
					{
						startIndex.set(j, m + 1);
						if (finalPLL.indexOf(otherValue) == -1)
							finalPLL.add(otherValue);
					}
					else if (maxValue == otherValue)
					{
						startIndex.set(j, m + 1);
						if (finalPLL.indexOf(maxValue) == -1)
							finalPLL.add(maxValue);
						found = true;
						break;
					}
					m++;
				}
			}
			if (!found)
				finalPLL.add(maxValue);
		}
		Collections.sort(finalPLL);
		printResult("DaatOr", queryTerms, finalPLL, comparisons);
	}
	
	public static void DaatAnd(Hashtable<String, LinkedList<Integer>> termPostingsList, List<String> queryTerms)
	{
		ArrayList<LinkedList<Integer>> qTPList = new ArrayList<LinkedList<Integer>>();
		
		LinkedList<Integer> finalPLL = new LinkedList<Integer>();
		LinkedList<Integer> tempLL;
		LinkedList<Integer> smallestPLL = new LinkedList<Integer>();
		
		int minPLen = Integer.MAX_VALUE, pLen = 0, minValue;
		int matchCount = 0, numOFPList = 0, comparisons = 0, lengthSPList = 0, numOfQterms = 0;
		String qTerm = "";
		List<Integer> startIndex = new ArrayList<Integer>();
		
		// Get the smallest postings list.
		numOfQterms = queryTerms.size();
		for (int s = 0; s < numOfQterms; s++)
		{
			qTerm = queryTerms.get(s);
			tempLL = new LinkedList<Integer>();
			tempLL = (LinkedList<Integer>)deepClone(termPostingsList.get(qTerm));
			if(tempLL == null)
				continue;	
			pLen = tempLL.size();
			if (minPLen > pLen)
			{
				minPLen = pLen;
				smallestPLL = tempLL;
			}	
			qTPList.add(tempLL);
		}
		qTPList.remove(smallestPLL);
		numOFPList = qTPList.size();
		lengthSPList = smallestPLL.size();
		for (int t = 0; t < numOFPList; t++)
			startIndex.add(0);
		
		for (int i = 0; i < lengthSPList; i++)
		{
			minValue = (int)smallestPLL.get(i);
			int m = 0;
			matchCount = 0;
			for (int j = 0; j < numOFPList; j++)
			{
				m = (int)startIndex.get(j);
				while (m <= (qTPList.get(j).size() - 1))
				{
					comparisons++;
					if (minValue == (int)qTPList.get(j).get(m))
					{
						startIndex.set(j, m + 1);
						matchCount++;
						break;
					}
					m++;
				}
			}
			if (matchCount == numOFPList)
				finalPLL.add(minValue);
		}
		Collections.sort(finalPLL);
		printResult("DaatAnd", queryTerms, finalPLL, comparisons);
	}
	
	public static void TaatAnd(Hashtable<String, LinkedList<Integer>> termPostingsList, List<String> queryTerms)
	{
		ArrayList<LinkedList<Integer>> qTPList = new ArrayList<LinkedList<Integer>>();
		
		LinkedList<Integer> finalPLL = new LinkedList<Integer>();
		LinkedList<Integer> tempLL;
		LinkedList<Integer> smallestPLL = new LinkedList<Integer>();
		
		int minPLen = Integer.MAX_VALUE, pLen = 0;
		int numOFPList = 0, comparisons = 0, lengthSPList = 0, lengthNewL = 0, matchCount = 0;
		
		// Get the smallest postings list.
		for (String qTerm : queryTerms)
		{
			tempLL = new LinkedList<Integer>();
			tempLL = (LinkedList<Integer>)deepClone(termPostingsList.get(qTerm));
			if (tempLL == null)
				continue;
			pLen = tempLL.size();
			if (minPLen > pLen)
			{
				minPLen = pLen;
				smallestPLL = tempLL;
			}	
			qTPList.add(tempLL);
		}
		qTPList.remove(smallestPLL);

		numOFPList = qTPList.size();
		lengthSPList = smallestPLL.size();

		for (int i = 0; i < lengthSPList; i++)
		{
			matchCount = 0;
			for (int j = 0; j < numOFPList; j++)
			{
				lengthNewL = qTPList.get(j).size();
				for(int k = 0; k < lengthNewL; k++)
				{
					comparisons++;
					if ((int)qTPList.get(j).get(k) == (int)smallestPLL.get(i))
					{
						matchCount++;
						break;
					}
				}
			}
			if (matchCount == numOFPList)
			{
				finalPLL.add(smallestPLL.get(i));
			}
		}
		Collections.sort(finalPLL);
		printResult("TaatAnd", queryTerms, finalPLL, comparisons);
	}
	
	public static void TaatOr(Hashtable<String, LinkedList<Integer>> termPostingsList, List<String> queryTerms)
	{
		List<LinkedList<Integer>> qTPList = new ArrayList<LinkedList<Integer>>();
		
		LinkedList<Integer> tempLL;
		LinkedList<Integer> finalPLL = new LinkedList<Integer>();
		LinkedList<Integer> largestPLL = new LinkedList<Integer>();
		List<Integer> startIndex = new ArrayList<Integer>();
		
		int numOFPList = 0, lengthLPList = 0, lengthNewL = 0, comparisons = 0, numOfQterms = 0;
		int maxPLen = Integer.MIN_VALUE, pLen = 0, otherValue = 0, maxValue = 0;
		String qTerm = "";
		boolean missing = true;
		
		numOfQterms = queryTerms.size();
		for (int s = 0; s < numOfQterms; s++)
		{
			qTerm = queryTerms.get(s);
			tempLL = new LinkedList<Integer>();
			tempLL = (LinkedList<Integer>)deepClone(termPostingsList.get(qTerm));
			if (tempLL == null)
				continue;
			pLen = tempLL.size();
			if (maxPLen < pLen)
			{
				maxPLen = pLen;
				largestPLL = tempLL;
			}	
			qTPList.add(tempLL);
		}
		qTPList.remove(largestPLL);
		
		numOFPList = qTPList.size();
		lengthLPList = largestPLL.size();
		
		for (int t = 0; t < numOFPList; t++)
			startIndex.add(0);
		for (int i = 0; i < lengthLPList; i++)
		{
			maxValue = (int)largestPLL.get(i);
			missing = true;
			for (int j = 0; j < numOFPList; j++)
			{
				lengthNewL = qTPList.get(j).size();
				for(int k = startIndex.get(j); k < lengthNewL; k++)
				{
					comparisons++;
					otherValue = (int)qTPList.get(j).get(k);
					if (otherValue > maxValue)
						break;
					else if (otherValue < maxValue)
					{
						startIndex.set(j, k + 1);
						if(finalPLL.indexOf(otherValue) == -1)
							finalPLL.add(otherValue);
					}
					else if (otherValue == maxValue)
					{
						startIndex.set(j, k + 1);
						if (finalPLL.indexOf(maxValue) == -1)
							finalPLL.add(maxValue);
						missing = false;
						break;
					}
				}
			}
			if(missing)
				finalPLL.add(maxValue);
		}
		
		// Append the rest of the elements
		for (int j = 0; j < numOFPList; j++)
		{
			lengthNewL = qTPList.get(j).size();
			for(int k = startIndex.get(j); k < lengthNewL; k++)
			{
				int value = (int)qTPList.get(j).get(k);
				if (finalPLL.indexOf(value) == -1)
					finalPLL.add(value);
			}
		}
		Collections.sort(finalPLL);
		printResult("TaatOr", queryTerms, finalPLL, comparisons);
	}
	
	public static Hashtable<String, LinkedList<Integer>> createPostingsList(Path indexPath)
	{
		IndexReader indexReader;
		MultiFields multiFields = null;
		Fields indexFields;
		Terms indexTerms;
		TermsEnum termsIterator;
		Hashtable<String, Terms> termFieldCombo = new Hashtable<String, Terms>();
		Enumeration tfcIterator;
		Hashtable<String, LinkedList<Integer>> termPostingsList = new Hashtable<String, LinkedList<Integer>>();

		Directory indexDirectory;
		try {
			indexDirectory = FSDirectory.open(indexPath);
			indexReader = DirectoryReader.open(indexDirectory);
			indexFields = multiFields.getFields(indexReader);
			for(String field : indexFields)
			{
				if (field.equals("_version_") || field.equals("id"))
					continue;
				indexTerms = multiFields.getTerms(indexReader, field);
				termFieldCombo.put(field, indexTerms);
			}
			tfcIterator = termFieldCombo.keys();
			while (tfcIterator.hasMoreElements())
			{
				String tField = (String) tfcIterator.nextElement();
				termsIterator = termFieldCombo.get(tField).iterator(); //term
				BytesRef term = termsIterator.next();
				while(term != null)
				{
					LinkedList<Integer> termposting = new LinkedList<Integer>();
					PostingsEnum postingsEnum = multiFields.getTermDocsEnum(indexReader, tField, term);
					while(postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS)
						termposting.add(postingsEnum.docID());
					termPostingsList.put(term.utf8ToString(), termposting);
					term = termsIterator.next();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return termPostingsList;
	}
	
	public static void printResult(String operation, List<String> terms, LinkedList<Integer> docIDs, int comparisons)
	{
		String termsString = "", docIDString = "";
		outputList.add(operation);
		for (String term : terms)
			termsString = termsString.concat(term + " ");
		outputList.add(termsString);
		for (int docID : docIDs)
			docIDString = docIDString.concat(String.valueOf(docID) + " ");
		if(docIDString.length() == 0)
			docIDString = "empty";
		outputList.add("Results: " + docIDString);
		outputList.add("Number of documents in results: " + docIDs.size());
		outputList.add("Number of comparisons: " + comparisons);
	}
	
	 public static Object deepClone(Object listObject) 
	 {
		 try 
		 {
		     ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		     ObjectOutputStream objectOS = new ObjectOutputStream(byteOS);
		     objectOS.writeObject(listObject);
		     ByteArrayInputStream byteIS = new ByteArrayInputStream(byteOS.toByteArray());
		     ObjectInputStream objectIS = new ObjectInputStream(byteIS);
		     return objectIS.readObject();
		 }
		 catch (Exception e) 
		 {
			 e.printStackTrace();
			 return null;
		 }
	 }
}



// References : 
// http://www.tutorialspoint.com/lucene/
// www.programcreek.com/java-api-examples/
// http://alvinalexander.com/java/java-deep-clone-example-source-code