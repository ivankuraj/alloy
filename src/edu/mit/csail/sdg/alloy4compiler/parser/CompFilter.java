/*
 * Alloy Analyzer
 * Copyright (c) 2007 Massachusetts Institute of Technology
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA,
 * 02110-1301, USA
 */

package edu.mit.csail.sdg.alloy4compiler.parser;

import java.util.LinkedList;
import java.io.Reader;
import java.io.IOException;
import java_cup_11a.runtime.Scanner;
import java_cup_11a.runtime.Symbol;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorAPI;
import static edu.mit.csail.sdg.alloy4compiler.parser.CompSym.*;

/**
 * This class sits between the lexer and the parser.
 *
 * <p>
 * Reason: there are 3 sets of "special tokens" that the lexer will not output.
 * But the Parser expects them. So this filter class observes the stream of
 * tokens, and intelligently merges or changes some primitive tokens into special tokens.
 *
 * <p>
 * For more details, refer to the main documentation.
 * But, very briefly, here are the 3 groups:
 *
 * <p>
 * (1) The lexer will generate only ALL, NO, LONE, ONE, SUM, SOME.
 * It will not output ALL2, NO2, LONE2, ONE2, SUM2, SOME2.
 * (The Filter class will change ONE into ONE2 when appropriate)
 *
 * <p>
 * (2) The lexer won't output NOTEQUALS, NOTIN, NOTLT, NOTLTE, NOTGT, NOTGTE.
 * Instead it outputs them as separate tokens (eg. "NOT" "EQUALS").
 * (The Filter class is used to merge them into a single "NOTEQUALS" token)
 *
 * <p>
 * (3) The lexer won't output the 15 special arrows (eg. ONE_ARROW_ONE)
 * Instead it outputs them as separate tokens (eg. "ONE", "ARROW", "ONE")
 * (The Filter class is used to merge them into a single "ONE_ARROW_ONE" token)
 */

final class CompFilter implements Scanner {

    // TODO: should double check that the filter rules in this file still matches the current Alloy4 grammar

    //===================== PHASE 1 ==================================================================================

    /** The underlying lexer. */
    private final CompLexer r;

    /** A list of tokens that we prefetched from the underlying lexer. */
    private final LinkedList<Symbol> undo = new LinkedList<Symbol>();

    /** Stores the latest token passed from phase 1 to phase 2. */
    private Symbol last = null;

    private final Symbol change(Symbol a,int b) { a.sym=b; return a; }

    /** Reads a token from the underlying lexer; if the undo list is not empty, we take it from there instead. */
    private Symbol myread() throws Err {
      if (!undo.isEmpty()) return undo.removeFirst();
      try {
          return r.next_token();
      } catch(IOException ex) {
          throw new ErrorAPI("IO error: "+ex.getMessage());
      }
    }

    /** Reads one or more tokens from the underlying lexer, transform them, then give it to phase 2. */
    public Symbol next_token() throws Err {
      Symbol a=myread(), b;
      if (a.sym==ID)
       {
        b=myread();
        if (b.sym==COLON)
         {
          Symbol c; c=myread();
          if (c.sym==RUN || c.sym==CHECK) {
              undo.add(0,a);
              undo.add(0,change(b,DOT));
              return last=c;
          }
          undo.add(0,c);
         }
        undo.add(0,b);
       }
      if (a.sym==NOT)
       {
        b=myread();
        if (b.sym==IN)     return last=change(a, NOTIN);
        if (b.sym==EQUALS) return last=change(a, NOTEQUALS);
        if (b.sym==LT)     return last=change(a, NOTLT);
        if (b.sym==LTE)    return last=change(a, NOTLTE);
        if (b.sym==GT)     return last=change(a, NOTGT);
        if (b.sym==GTE)    return last=change(a, NOTGTE);
        undo.add(0,b); return last=a;
       }
      if (a.sym==ARROW) return last=arrow(a,0);
      if (a.sym==SET)
       {
        if ((b=myread()).sym==ARROW) return last=arrow(a,0);
        undo.add(0,b);
       }
      if (a.sym==LONE)
       {
        if ((b=myread()).sym==ARROW) return last=arrow(a,1);
        if (last==null || last.sym!=COLON) return last=decl(a,b,LONE2);
        undo.add(0,b);
       }
      if (a.sym==ONE)
       {
        if ((b=myread()).sym==ARROW) return last=arrow(a,2);
        if (last==null || last.sym!=COLON) return last=decl(a,b,ONE2);
        undo.add(0,b);
       }
      if (a.sym==SOME)
       {
        if ((b=myread()).sym==ARROW) return last=arrow(a,3);
        if (last==null || last.sym!=COLON) return last=decl(a,b,SOME2);
        undo.add(0,b);
       }
      if (last==null || last.sym!=COLON)
       {
        if (a.sym==NO) return last=decl(a, myread(), NO2);
        if (a.sym==ALL) return last=decl(a, myread(), ALL2);
        if (a.sym==SUM) return last=decl(a, myread(), SUM2);
       }
      return last=a;
    }

    private final int arrows[] = { // The element order is important
      ANY_ARROW_LONE, ANY_ARROW_ONE, ANY_ARROW_SOME, ARROW,
      LONE_ARROW_LONE, LONE_ARROW_ONE, LONE_ARROW_SOME, LONE_ARROW_ANY,
      ONE_ARROW_LONE, ONE_ARROW_ONE, ONE_ARROW_SOME, ONE_ARROW_ANY,
      SOME_ARROW_LONE, SOME_ARROW_ONE, SOME_ARROW_SOME, SOME_ARROW_ANY
    };

    private Symbol arrow(Symbol a,int m) throws Err {
      Symbol b=myread();
      if (b.sym==LONE) return change(a,arrows[m*4  ]);
      if (b.sym==ONE ) return change(a,arrows[m*4+1]);
      if (b.sym==SOME) return change(a,arrows[m*4+2]);
      if (b.sym==SET ) return change(a,arrows[m*4+3]);
      undo.add(0,b);   return change(a,arrows[m*4+3]);
    }

    private Symbol decl(Symbol a,Symbol b,int c) throws Err {
        LinkedList<Symbol> temp=new LinkedList<Symbol>();
        if (b.sym==DISJ || b.sym==PART || b.sym==EXH) {temp.add(b); b=myread();}
        temp.add(b);
        if (b.sym==ID) {
            while(true) {
                temp.add(b=myread());
                if (b.sym==COLON) {change(a,c);break;}
                if (b.sym!=COMMA) break;
                temp.add(b=myread());
                if (b.sym!=ID) break;
            }
        }
        for(int i=temp.size()-1; i>=0; i--) undo.add(0, temp.get(i));
        return a;
    }

    /** Construct a filter for the tokens from the given file. */
    public CompFilter(boolean allowDollar, final String filename, int lineOffset, Reader i) throws Err {
        r=new CompLexer(i);
        r.alloy_dollar=allowDollar;
        r.alloy_filename=filename;
        r.alloy_lineoffset=lineOffset;
    }
}
