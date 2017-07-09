package edu.stanford.muse.ner;

/**
 * Created by vihari on 23/10/15.
 */
public class Entity {
    public String entity;
    public double score;
    public int freq;
    public Entity(String entity, double score){
        this.entity = entity;
        this.score = score;
        this.freq = 1;
    }

    public String toString(){
        return "Entity:"+entity+", Freq:"+freq+", Score:"+score;
    }

    public int hashCode(){
        return ("E:"+entity+", F:"+freq+", S:"+score).hashCode();
    }
}
