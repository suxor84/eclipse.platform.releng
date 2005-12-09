/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.releng.tools.preferences.RelEngCopyrightConstants;

public class AdvancedCopyrightComment {
	private static final String DATE_VAR = "${date}"; //$NON-NLS-1$
	private static final String NEW_LINE = "\n"; //$NON-NLS-1$

    public static final int UNKNOWN_COMMENT = -1;
    public static final int JAVA_COMMENT = 1;
    public static final int PROPERTIES_COMMENT = 2;

    private int commentStyle = 0;
    private int creationYear = -1;
    private int revisionYear = -1;
    private List contributors;
    private String preYearComment = null;
    private String postYearComment = null;

    private AdvancedCopyrightComment(int commentStyle, int creationYear, int revisionYear, List contributors, String preYearComment, String postYearComment) {
        this.commentStyle = commentStyle;
       	this.creationYear = creationYear == -1 ? getPreferenceStore().getInt(RelEngCopyrightConstants.CREATION_YEAR_KEY) : creationYear;
        this.revisionYear = revisionYear;
        this.contributors = contributors;
        this.preYearComment = preYearComment;
        this.postYearComment = postYearComment;
    }
    
    private AdvancedCopyrightComment(int commentStyle, int creationYear, int revisionYear, List contributors) {
    	this(commentStyle, creationYear, revisionYear, contributors, null, null);
    }

    public static AdvancedCopyrightComment defaultComment(int commentStyle) {
        return new AdvancedCopyrightComment(commentStyle, -1, -1, null);
    }
    
    public int getRevisionYear() {
        return revisionYear == -1 ? creationYear : revisionYear;
    }

    public void setRevisionYear(int year) {
        if (revisionYear != -1 || creationYear != year)
            revisionYear = year;
    }

    private static String getLinePrefix(int commentStyle) {
        switch(commentStyle) {
        case JAVA_COMMENT:
            return " * ";  //$NON-NLS-1$
        case PROPERTIES_COMMENT:
            return "# "; //$NON-NLS-1$
        default:
            return null;
        }
	}

	private void writeCommentStart(PrintWriter writer) {
	    switch(commentStyle) {
	    case JAVA_COMMENT:
			writer.println("/*******************************************************************************"); //$NON-NLS-1$
			break;
	    case PROPERTIES_COMMENT:
		    writer.println("###############################################################################"); //$NON-NLS-1$
		    break;
	    }
	}

	private void writeContributions(PrintWriter writer, String linePrefix) {
		writer.println(linePrefix);
		writer.println(linePrefix + "Contributors:"); //$NON-NLS-1$

		if (contributors == null || contributors.size() <= 0)
		    writer.println(linePrefix + "    IBM Corporation - initial API and implementation"); //$NON-NLS-1$
		else {
			Iterator i = contributors.iterator();
			while (i.hasNext())
			    writer.println(linePrefix + "    " + (String)i.next());  //$NON-NLS-1$
		}
	}

	private void writeCommentEnd(PrintWriter writer) {
	    switch(commentStyle) {
	    case JAVA_COMMENT:
			writer.println(" *******************************************************************************/"); //$NON-NLS-1$
			break;
	    case PROPERTIES_COMMENT:
		    writer.println("###############################################################################"); //$NON-NLS-1$
		    break;
	    }
	}
	
	/**
	 * Get the copyright tool preference store
	 * @return
	 */
    private static IPreferenceStore getPreferenceStore() {
    	return RelEngPlugin.getDefault().getPreferenceStore();
    }
	
	/**
	 * Get the copyright statement in form of an array of Strings where
	 * each item is a line of the copyright statement.
	 * @return String[]
	 */
	private static String[] getLegalLines() {
		StringTokenizer st = new StringTokenizer(getPreferenceStore().getString(RelEngCopyrightConstants.COPYRIGHT_TEMPLATE_KEY), NEW_LINE, true);
		ArrayList lines = new ArrayList();
		String previous = NEW_LINE;
		while (st.hasMoreTokens()) {
			String current = st.nextToken();
			// add empty lines to array as well
			if (NEW_LINE.equals(previous)) {
				lines.add(current);
			}
			previous = current;
		}
		String[] stringLines = new String[lines.size()];
		stringLines = (String[])lines.toArray(stringLines); 
		return stringLines;
	}
	
	/**
	 * Return the body of this copyright comment or null if it cannot be built.
	 */
	public String getCopyrightComment() {
		// instead of overwriting an existing comment, just try to insert the new year
		// disable fix up existing copyright till it works better
//		if ((preYearComment != null || postYearComment != null) && (!getPreferenceStore().getBoolean(RelEngCopyrightConstants.FIX_UP_EXISTING_KEY))) {
		if ((preYearComment != null || postYearComment != null)) {
			String copyrightString = preYearComment == null ? "" : preYearComment; //$NON-NLS-1$
			copyrightString = copyrightString + creationYear;
			
			if (revisionYear != -1 && revisionYear != creationYear)
		        copyrightString = copyrightString + ", " + revisionYear; //$NON-NLS-1$
			
			String endString = postYearComment == null ? "" : postYearComment; //$NON-NLS-1$
			copyrightString = copyrightString + endString;
			return copyrightString;
		}
		
	    String linePrefix = getLinePrefix(commentStyle);
	    if (linePrefix == null)
	        return null;

	    StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		try {
		    writeCommentStart(writer);
			writeLegal(writer, linePrefix);
			// dont do anything special with contributors right now
//			writeContributions(writer, linePrefix);
		    writeCommentEnd(writer);

			return out.toString();
		} finally {
		    writer.close();
		}
	}
	
	/**
	 * Write out the copyright statement, line by line, adding in the created/revision
	 * year as well as comment line prefixes.
	 * 
	 * @param writer
	 * @param linePrefix
	 */
	private void writeLegal(PrintWriter writer, String linePrefix) {
		String[] legalLines = getLegalLines();
		for (int i=0; i < legalLines.length; ++i) {
			String currentLine = legalLines[i];
			int offset = currentLine.indexOf(DATE_VAR);
			// if this is the line, containing the ${date}, add in the year
			if (offset > -1) {
				writer.print(linePrefix + currentLine.substring(0, offset)+creationYear);
				if (revisionYear != -1 && revisionYear != creationYear)
			        writer.print(", " + revisionYear); //$NON-NLS-1$
				writer.println(currentLine.substring(offset+DATE_VAR.length(), currentLine.length()));
			} else {
				// just write out the line
				if (NEW_LINE.equals(currentLine)) {
					// handle empty lines
					writer.print(linePrefix + currentLine);
				} else {
					writer.println(linePrefix + currentLine);
				}
			}
		}
	}
	
    /**
     * Create an instance the same as the argument comment but with the revision year
     * updated if needed.  Return the default comment if the argument comment is null
     * or an empty string.  Return null if the argument comment is not recognized as
     * an IBM copyright comment.
     */
    public static AdvancedCopyrightComment parse(BlockComment comment, int commentStyle) {
    	AdvancedCopyrightComment copyright = null;
        
    	if (comment == null) {
    		copyright = defaultComment(commentStyle);
    	} else {
    		// To make the comment search a little more flexible, the parse algorithm will 
    		// only parse the line containing the ${date}
	   	    String body = comment.getContents();
	   	    
	   	    // find the line with ${date}
	   	    String[] legalLines = getLegalLines();
	   	    int i = 0;
	   	    int yearOffset = -1;
	   	    while (i < legalLines.length && yearOffset == -1) {
	   	    	String line = legalLines[i];
	   	    	yearOffset = line.indexOf(DATE_VAR);
	   	    	++i;
	   	    }
	   	    // ${date} found
	   	    if (yearOffset != -1) {
	   	    	String yearLine = legalLines[i-1];
	   	   	    // split that line up and just search for the contents before and after
	   	    	// NOTE: this won't really work well if the text surrounding the year is
	   	    	// generic, or if the year is at the beginning or end of the line
	   	    	String preYear = yearLine.substring(0, yearOffset);
	   	    	String postYear = yearLine.substring(yearOffset+DATE_VAR.length(), yearLine.length());
	   	    	
	   	    	int preYearOffset = body.indexOf(preYear);
	   	    	if (preYearOffset != -1) {
	   	    		int postYearOffset = body.indexOf(postYear, preYearOffset);
	   	    		if (postYearOffset != -1) {
	   	    	   	    // then you know between that is the year
	   	    			String yearRange = body.substring(preYearOffset+preYear.length(), postYearOffset);
	   	    	   	    int comma = yearRange.indexOf(","); //$NON-NLS-1$
	
	   	    	   	    String startStr = comma == -1 ? yearRange : yearRange.substring(0, comma);
	   	    	   	    String endStr = comma == -1 ? null : yearRange.substring(comma + 1);
	
	   	    	   	    int startYear = -1;
	   	    	   	    if (startStr != null)
	   	    		   	    try {
	   	    		   	        startYear = Integer.parseInt(startStr.trim());
	   	    		   	    } catch(NumberFormatException e) {
	   	    		   	        // do nothing
	   	    		   	    }
	
	   	    	   	    int endYear = -1;
	   	    	   	    if (endStr != null) {
	   	    	   	        try {
	   	    	   	            endYear = Integer.parseInt(endStr.trim());
	   	    	   	        } catch(NumberFormatException e) {
	   	    	   	            // do nothing
	   	    	   	        }
	   	    	   	    }
	   	    	   	    // save the copyright comment's contents before and after the year so that
	   	    	   	    // the comment will remain untouched rather than overwritten if the template is
	   	    	   	    // almost the same
	   	    	   	    String pre = body.substring(0, preYearOffset+preYear.length());
	   	    	   	    String post = body.substring(postYearOffset);
	   	    	   	     
	   	    	   	    copyright = new AdvancedCopyrightComment(commentStyle, startYear, endYear, null, pre, post);
	   	    		}
	   	    	}
	   	    }
    	}

    	// don't do anything special with contributors right now
//   	    int contrib = body.indexOf("Contributors:", start); //$NON-NLS-1$
//   	    String contribComment = body.substring(contrib);
//   	    StringTokenizer tokens = new StringTokenizer(contribComment, "\r\n"); //$NON-NLS-1$
//   	    tokens.nextToken();
//   	    ArrayList contributors = new ArrayList();
//        String linePrefix = getLinePrefix(commentStyle);
//   	    while(tokens.hasMoreTokens()) {
//   	        String contributor = tokens.nextToken();
//   	        if (contributor.indexOf("***********************************") == -1 //$NON-NLS-1$
//   	         && contributor.indexOf("###################################") == -1) { //$NON-NLS-1$
//   	            int c = contributor.indexOf(linePrefix);
//   	            if (c != -1)
//   	                contributor = contributor.substring(c + linePrefix.length());
//   	            contributors.add(contributor.trim());
//   	        }
//   	    }
//
//        return new IBMCopyrightComment(commentStyle, startYear, endYear, contributors);
    	return copyright;
    }
}