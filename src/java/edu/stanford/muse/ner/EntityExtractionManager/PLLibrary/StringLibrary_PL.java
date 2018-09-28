package edu.stanford.muse.ner.EntityExtractionManager.PLLibrary;

import alice.tuprolog.Library;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringLibrary_PL extends Library {

    public boolean string_lower_2(Term source, Term dest){
        String st = source.toString().toLowerCase();
        return  unify(dest,new Struct(st));

    }

    public boolean split_string_4(Term str, Term sep, Term pad, Term lst){

        String st = str.toString();
        String seps = sep.toString();
        List<Struct> res = Arrays.stream(st.split(seps)).map(f->new Struct(f)).collect(Collectors.toList());
        Struct[] tmp = new Struct[0];
        return  unify(lst,new Struct(res.toArray(tmp)));

    }
    public String getName(){
        return "StringLibrary";
    }
}
