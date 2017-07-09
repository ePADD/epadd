package edu.stanford.muse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by vihari on 24/02/16.
 * Similar in spirit to opennlp.tools.util
 * used to represent the chunks recognized by a NER model, @see edu.stanford.muse.ner.model.NERModel interface
 */
public class Span implements java.io.Serializable {
    public int start, end;
    public String text;
    public short type = -1;
    public float typeScore = 0f;
    public static Log log = LogFactory.getLog(Span.class);


    /**
     * @param start - The start offset of chunk in the content
     * @param end   - end offset
     */
    public Span(String chunk, int start, int end) {
        this.text = chunk;
        this.start = start;
        this.end = end;
    }

    public Span(String chunk, int start, int end, short type) {
        this.text = chunk;
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public void setType(short type, float score) {
        this.type = type;
        this.typeScore = score;
    }

    public short getType() {
        return type;
    }

    public String getText() {return text;}

    @Override
    public String toString() {
        return "[" + this.start + ".." + this.end + ")" + " " + this.type + " " + this.typeScore;
    }

    /**
     * Prints in parse friendly manner
     */
    public String parsablePrint() {
        return this.text + ";" + this.start + ";" + this.end + ";" + this.type + ";" + this.typeScore;
    }

    /**
     * Given a text printed by parsablePrint, parses it and returns handle to the initialized object
     */
    public static Span parse(String text) {
        if (text == null) {
            log.warn("Found null content while parsing entity spans!!!");
            return null;
        }
        if(text.equals(""))
            return null;

        String[] fields = text.split(";");
        if (fields.length != 7 && fields.length != 5) {
            log.warn("Unexpected number of fields in content: " + text);
            return null;
        }
        Span chunk = new Span(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
        chunk.setType(Short.parseShort(fields[3]), Float.parseFloat(fields[4]));
        return chunk;
    }

    /**
     * This method investigates the best way to store Span objects that balances both space and time requirements
     * Experiments with text, serialized, and gzipped serialized output formats and compares the disk space and parse time of each*/
    static void experimentio() {
        java.util.List<java.util.List<Span>> spans = new java.util.ArrayList<>();
        java.util.Random rand = new java.util.Random();
        int MAX = 1000;
        for (int i = 0; i < MAX; i++) {
            java.util.List<Span> lst = new java.util.ArrayList<>();
            for(int j=0;j<20;j++) {
                StringBuilder r = new StringBuilder("");
                int l = rand.nextInt(100) + 20;
                for (int ci = 0; ci < l; ci++)
                    r.append((char) (rand.nextInt('z' - 'a' + 1) + 'a'));
                int rs = rand.nextInt(100);
                int re = l + rs;
                Span sp = new Span(r.toString(), rs, re);
                sp.setType((short) (rand.nextInt(128)), rand.nextFloat());
                lst.add(sp);
            }
            spans.add(lst);
        }
        String sep = java.io.File.separator;
        String tmpFldr = System.getProperty("java.io.tmpdir") + sep;
        //write spans to individual files in each of the folders
        //write three files -- .txt, .ser, .ser.gz
        String fldrs[] = new String[]{tmpFldr + "txt" + sep, tmpFldr + "ser" + sep, tmpFldr + "ser.gz" + sep};
        for (String fn : fldrs) {
            System.out.println("Folder: " + fn);
            java.io.File fldr = new java.io.File(fn);

            if (!fldr.exists())
                fldr.mkdir();
        }
        try {
            for (int si = 0; si < spans.size(); si++) {
                java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(fldrs[0] + si + ".txt"));
                StringBuilder sb = new StringBuilder();
                spans.get(si).forEach(sp->sb.append(sp.parsablePrint()+":::"));
                bw.write(sb.toString());
                bw.close();

                java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(bs);
                oos.writeObject(spans.get(si));
                oos.close();
                bw.close();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(fldrs[1] + si + ".ser");
                fos.write(bs.toByteArray());
                fos.close();

                bs = new java.io.ByteArrayOutputStream();
                java.util.zip.GZIPOutputStream gos = new java.util.zip.GZIPOutputStream(bs);
                oos = new java.io.ObjectOutputStream(gos);
                oos.writeObject(spans.get(si));
                oos.close();
                gos.close();
                bs.close();
                fos = new java.io.FileOutputStream(fldrs[2] + si + ".ser.gz");
                fos.write(bs.toByteArray());
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception writing span");
        }

        for (String fldrN : fldrs)

        {
            int size = 0;
            long timeParse = 0;
            java.io.File fldr = new java.io.File(fldrN);
            java.io.File[] files = fldr.listFiles();
            for (java.io.File file : files) {
                size += file.length();
                if (fldrN.endsWith("txt" + sep)) {
                    try {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                        String line;
                        while ((line = br.readLine()) != null) {
                            long st = System.nanoTime();
                            String[] toks = line.split(":::");
                            for(String tok: toks)
                                Span.parse(tok);
                            timeParse += (System.nanoTime() - st);
                        }
                        br.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (fldrN.endsWith("ser" + sep)) {
                    try {
                        java.io.FileInputStream fi = new java.io.FileInputStream(file);
                        byte[] bytes = new byte[(int) file.length()];
                        fi.read(bytes);
                        long st = System.nanoTime();
                        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes));
                        java.util.List<Span> sp = (java.util.List<Span>) ois.readObject();
                        timeParse += System.nanoTime() - st;
                        ois.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (fldrN.endsWith("ser.gz" + sep)) {
                    try {
                        java.io.FileInputStream fi = new java.io.FileInputStream(file);
                        byte[] bytes = new byte[(int) file.length()];
                        int b, bi = 0;
                        while ((b = fi.read()) != -1)
                            bytes[bi++] = (byte) b;
                        //fi.read(bytes);
                        long st = System.nanoTime();
                        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes)));
                        java.util.List<Span> sp = (java.util.List<Span>) ois.readObject();
                        timeParse += System.nanoTime() - st;
                        ois.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println(
                    "-----------------------\n" +
                            "Folder: " + fldrN +
                            "\nTime to parse #" + files.length + " spans " + (float) (timeParse * 1E-6) + "ms." +
                            "\nAverage size of span: " + (size / files.length) + "bytes" +
                            "\n----------------------");
        }
    }

    public static void main(String[] args) {
        experimentio();
    }
}
