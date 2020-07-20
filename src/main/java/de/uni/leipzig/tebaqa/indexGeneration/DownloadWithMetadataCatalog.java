package de.uni.leipzig.tebaqa.indexGeneration;

import com.clarkparsia.pellet.sparqldl.model.QueryResult;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.turtle.TurtleParser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.mindswap.pellet.utils.FileUtils.readAll;

public class DownloadWithMetadataCatalog {
    private static JSONArray readJsonFromUrl(String urlstring) throws IOException, JSONException {
        URL url = new URL(urlstring);
        URLConnection conn = url.openConnection();
        //conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        InputStream is = conn.getInputStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }
    public static void readTurtleMetadataCatalogFromUrl(String urlstring) throws IOException, JSONException, RDFParseException, RDFHandlerException {
        /*URL url = new URL(urlstring);

        URLConnection conn = url.openConnection();*/
        //conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        FileUtils.cleanDirectory(new File("limboDatasetsFromCat"));
        String query="SELECT DISTINCT ?ds ?dist ?download { " +
                "?ds <http://www.w3.org/ns/dcat#distribution> ?dist." +
                "?dist <http://www.w3.org/ns/dcat#downloadURL> ?download.\n" +
                " ?ds a <http://www.w3.org/ns/dcat#Dataset>; \n" +
                "    <http://purl.org/dc/terms/identifier> ?id ;\n" +
                "    <http://dataid.dbpedia.org/ns/core#group> ?group ;\n" +
                "    <http://purl.org/dc/terms/issued> ?ts ;\n" +
                "\t<http://www.w3.org/ns/dcat#distribution> ?dist .\n" +
                " \n" +
                " FILTER(?group=\"org.limbo\") \n" +
                " # filter out any dataset duplicates with earlier released dates, this should obtain the most current datasets\n" +
                " FILTER(!EXISTS {\n" +
                " ?ds2 <http://dataid.dbpedia.org/ns/core#artifact> ?art . \n" +
                " ?ds <http://dataid.dbpedia.org/ns/core#artifact> ?art . \n" +
                " ?ds2 <http://purl.org/dc/terms/issued> ?ts2 .\n" +
                " FILTER (?ts2 > ?ts) })\n" +
                "\n" +
                "}\n";
        Model m = FileManager.get().loadModel(urlstring);
        QueryExecution qe = QueryExecutionFactory.create(query,m);
        ResultSet rs=qe.execSelect();
        int index=0;
        while (rs.hasNext()){
            QuerySolution s=rs.nextSolution();
            System.out.println(s.get("ds")+" "+s.get("?dist")+" "+s.get("download"));
            FileUtils.copyURLToFile(new URL(s.get("download").toString()),new File("limboDatasetsFromCat/"+(index++)+".ttl"));
        }
        qe.close();
        /*InputStream is = conn.getInputStream();
        RDFParser parser;
        RDFHandlerBase handler= new DownloadWithMetadataCatalog.StatementHandler();
        parser = new TurtleParser();

        parser.setRDFHandler(handler);
        parser.setStopAtFirstError(false);
        parser.parse(is,"UTF-8");
        is.close();*/
    }
     public static void getFilesFromMetadataCatalog(String metadataurl){

         System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        try {
            FileUtils.cleanDirectory(new File("limboDatasets"));
            JSONArray metadatacatalog=readJsonFromUrl(metadataurl);
            for(int i=0;i<metadatacatalog.length();i++){
                JSONArray distributions=metadatacatalog.getJSONObject(i).getJSONObject("s").getJSONArray("distribution");
                for(int j=0;j< distributions.length();j++){
                    String url=distributions.getJSONObject(j).getJSONArray("downloadURL").getJSONObject(0).getString("id");
                    String filename=distributions.getJSONObject(j).getJSONArray("localId").getString(0);
                    FileUtils.copyURLToFile(new URL(url),new File("limboDatasets/"+i+"_"+j+"_"+filename));
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
