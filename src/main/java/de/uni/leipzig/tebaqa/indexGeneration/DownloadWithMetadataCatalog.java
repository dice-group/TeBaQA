package de.uni.leipzig.tebaqa.indexGeneration;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

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
     public static void getFilesFromMetadataCatalog(String metadataurl){

         System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");
        try {
            FileUtils.cleanDirectory(new File("limboDatasets"));
            JSONArray metadatacatalog=readJsonFromUrl(metadataurl);
            for(int i=0;i<metadatacatalog.length();i++){
                String url=(metadatacatalog.getJSONObject(i).getJSONObject("s").getJSONArray("distribution").getJSONObject(0)
                        .getJSONArray("downloadURL").getJSONObject(0).getString("id"));
                String filename=metadatacatalog.getJSONObject(i).getJSONObject("s").getJSONArray("distribution").getJSONObject(0)
                        .getJSONArray("localId").getString(0);
                FileUtils.copyURLToFile(new URL(url),new File("limboDatasets/"+i+"_"+filename));
            }
            //System.out.println(metadatacatalog);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
