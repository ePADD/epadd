/**
 * Created by hangal on 10/10/17.
 */
package expt;

import java.io.*;

class Serialization implements Serializable {
    private final static long serialVersionUID = 8496087002446756002L;

    int a, b;
    transient int c;


    public String toString() {
        return (a + " " + b + " " + c);
    }

    public static void main (String args[]) throws IOException, ClassNotFoundException {
        Serialization s;
        if ("read".equals(args[0])) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("/tmp/out.ser"));
            s = (Serialization) ois.readObject();
            ois.close();
        } else {
            s = new Serialization();
            s.a = 1;
            s.b = 2;
            s.c = 3;
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("/tmp/out.ser"));
            oos.writeObject(s);
            oos.close();
        }

        System.out.println (s);
    }

}
