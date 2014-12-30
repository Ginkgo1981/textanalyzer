package asia.wildfire;

import asia.wildfire.utils.RedisUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.trigonic.jedis.NamespaceJedis;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wltea.analyzer.IKSegmentation;
import org.wltea.analyzer.Lexeme;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebService
public class Featurer {

    static HashMap<String, String> featureMap = new HashMap<String, String>();;

    static long featureMapTimestamp = 0;

    static String SEPARATOR = "|";
    private static Map<String, Double> priorMap = Maps.newHashMap();
    private static Map<String, Map<String, Double>> linkihoodMap = Maps.newHashMap();
    private static List<String> tagList = Lists.newArrayList();
    private static NamespaceJedis jedis = null;

    static{
//        jedis = RedisUtils.initJedis();
//        priorMap = RedisUtils.getPriorValueMap();
//        linkihoodMap = RedisUtils.getLikelihoodValueMap();
//        tagList = RedisUtils.getTagList();
        initFeatureMap();

    }

    public String doFeature(List<String> contents) {
        try {
            StringBuffer returned = new StringBuffer("");
            int count = contents.size();
            for (String content : contents) {
                HashMap<String, Integer> countMap = new HashMap<String, Integer>();
                if (content == null || content.length() == 0){
                    if (count > 1) {
                        returned.append("|");
                    }
                    count--;
                    continue;
                }
                String keyword = null;
                StringBuffer sb = new StringBuffer();
                try {
                    JSONObject jo = new JSONObject(content);
                    JSONArray jArray = new JSONArray(
                            parser(Arrays.asList(jo.getString("body"))));
                    for (int j = 0; j < jArray.length(); j++) {
                        JSONObject jo_token = jArray.getJSONObject(j);
                        if (jo_token == null)
                            continue;
                        keyword = jo_token.getString("text");
                        sb.append(SEPARATOR);
                        sb.append(keyword);
                    }
                    sb.append(SEPARATOR);
                } catch (Exception e) {
                    continue;
                }
                content = sb.toString();
                for (Map.Entry<String, String> entry : featureMap.entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    Pattern p = Pattern.compile(k.replaceAll("[|]", "[|]"));
                    Matcher m = p.matcher(content.toUpperCase());
                    while (m.find()) {
                        if (countMap.containsKey(v)) {
                            countMap.put(v, countMap.get(v) + 1);
                        } else {
                            countMap.put(v, 1);
                        }
                    }
                }
                int i = 0;
                for (String key : countMap.keySet()) {
                    if (i > 0) {
                        returned.append(",");
                    }
                    returned.append(key.toString());
                    returned.append("=");
                    returned.append(countMap.get(key).toString());
                    i++;
                }
                if (count > 1) {
                    returned.append("|");
                }
                count--;
            }
//            System.out.println(returned.toString());
            return returned.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void initFeatureMap(){
        File f= new File("./features-newest.txt");
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            while(line!=null){
                String[] kv = line.split("\t");
                if(kv.length == 2){
                    featureMap.put("|"+kv[0].toUpperCase()+"|", kv[1].toUpperCase());
                }
                line = br.readLine();
            }
            System.out.println(featureMap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 延迟加载初始化
     */
    private static void initRedisData() {
        if (null == priorMap || priorMap.size() <= 0) {
            priorMap = RedisUtils.getPriorValueMap();
        }
        if (null == linkihoodMap || linkihoodMap.size() <= 0) {
            linkihoodMap = RedisUtils.getLikelihoodValueMap();
        }
        if (null == tagList || tagList.size() <= 0) {
            tagList = RedisUtils.getTagList();
        }
    }

//	private synchronized void initFeatureMap() {
//		if (System.currentTimeMillis() - featureMapTimestamp < 1000 * 60 * 60 * 24) {
//			return;
//		}
//		String u = COUCHDB_FEATURE
//				+ "_design/influencerforce/_view/feature-rules";
//		String res = getRequest(u);
//		JSONObject jo;
//		try {
//			jo = new JSONObject(res);
//			JSONArray ja = jo.getJSONArray("rows");
//			if (ja == null || ja.length() == 0)
//				return;
//			JSONObject jo_env = null;
//			JSONArray ja_key = null;
//			JSONArray ja_value = null;
//			featureMap = new HashMap<String, String>();
//			for (int i = 0; i < ja.length(); i++) {
//				// get each rule
//				jo_env = ja.getJSONObject(i);
//				ja_key = jo_env.getJSONArray("key");
//				if (ja_key == null || ja_key.length() == 0
//						|| ja_key.get(0) == null) {
//					continue;
//				}
//				if ("ngram".equals(ja_key.getString(0))) {
//					ja_value = jo_env.getJSONArray("value");
//					if (ja_value == null || ja_value.length() == 0) {
//						continue;
//					}
//					StringBuffer sb = new StringBuffer();
//					for (int j = 0; j < ja_value.length(); j++) {
//						sb.append(SEPARATOR);
//						sb.append(ja_value.getString(j));
//					}
//					sb.append(SEPARATOR);
//					String rules = sb.toString();
//					try {
//						if (null != ja_key.getString(2))
//							featureMap.put(rules, ja_key.getString(2));
//					} catch (Exception e) {
//						System.out.println("1");
//						// TODO: handle exception
//					}
//				} else if ("regexp".equals(ja_key.getString(0))) {
//					// TODO
//				}
//
//			}
//			featureMapTimestamp = System.currentTimeMillis();
//			System.out.println("  ## feature rules:" + featureMap.toString());
//		} catch (JSONException e) {
//			e.printStackTrace();
//			System.out.println("++++++++++++++++++++++++++++++++++");
//			System.out.println("++++++++++++++++++++++++++++++++++");
//			System.out.println("++++++++++++++++++++++++++++++++++");
//			System.out.println("++++++++++++++++++++++++++++++++++");
//			System.out.println("++++++++++++++++++++++++++++++++++");
//			System.out.println(System.currentTimeMillis());
//		}
//	}

    protected static String COUCHDB_FEATURE = "http://175.41.159.199:5985/features/";

    protected String getRequest(String u) {
        try {
            URL url = new URL(u);
            InputStream response = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response));
            StringBuffer jb = new StringBuffer();
            for (String line; (line = reader.readLine()) != null;) {
                jb.append(line);
            }
            reader.close();
            return jb.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public String parser(List<String> str) {
        if (str == null || str.isEmpty() || "".equals(str.get(0))){
            return "";
        }

        StringReader reader = new StringReader(str.get(0));
        IKSegmentation ik = new IKSegmentation(reader, true);
        Lexeme lexeme = null;
        JSONObject jObj = null;
        JSONArray jArray = new JSONArray();
        // String[] parameters = new String[]{ "begin", "length", "text"};
        try {
            while ((lexeme = ik.next()) != null) {
                jObj = new JSONObject();
                jObj.put("begin", lexeme.getBegin());
                jObj.put("length", lexeme.getLength());
                jObj.put("text", lexeme.getLexemeText());
                jArray.put(jObj);
            }
        } catch (Exception e) {

        }

        return jArray.toString();
    }

    public String doSegmentation(List<String> contents) {
        System.out.println(System.currentTimeMillis());
        System.out.println("do feature");
        System.out.println(contents.size());
        try {
            StringBuffer returned = new StringBuffer("");
            int count = contents.size();
            for (String content : contents) {
                HashMap<String, Integer> countMap = new HashMap<String, Integer>();
                if (content == null || content.length() == 0){
                    if (count > 1) {
                        returned.append("|");
                    }
                    count--;
                    continue;
                }
                try {
                    JSONObject jo = new JSONObject(content);
                    JSONArray jArray = new JSONArray(
                            parser(Arrays.asList(jo.getString("body"))));
                    for (int j = 0; j < jArray.length(); j++) {
                        JSONObject jo_token = jArray.getJSONObject(j);
                        if (jo_token == null)
                            continue;
                        String keyword = jo_token.getString("text");
                        if (countMap.containsKey(keyword)) {
                            countMap.put(keyword, countMap.get(keyword) + 1);
                        } else {
                            countMap.put(keyword, 1);
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
                int j = 0;
                for (Entry<String, Integer> entry : countMap.entrySet()) {
                    if(j > 0){
                        returned.append(",");
                    }
                    returned.append(entry.getKey());
                    returned.append("=");
                    returned.append(entry.getValue());
                    j++;
                }
                if (count > 1) {
                    returned.append("|");
                }
                count--;
            }

            System.out.println(returned.toString());
            return returned.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * move sentiment analytis to text analut
     *
     */
    public static Map<String,Double> predictedTag(Map<String, Integer> featureMap) {
        //初始化redis数据
        initRedisData();

        //最终返回结果
        Map<String,Double> resultMap = Maps.newHashMap();
        Map<String, Double> predictedMap = new HashMap<String, Double>();
        Set<String> featureSet = featureMap.keySet();
        for (String tag : tagList) {
            predictedMap.put(tag, priorMap.get(tag) == null ? 0 : priorMap.get(tag));
            // 循环feature
            for (String feature : featureSet) {
                double p_dou = predictedMap.get(tag);
                if (linkihoodMap.get(tag) != null) {
                    Double link_dou = linkihoodMap.get(tag).get(feature);
                    if (null == linkihoodMap.get(tag).get(feature)) {
                        link_dou = Double.valueOf(0);
                    }
                    p_dou += link_dou * (1 + Math.log(featureMap.get(feature)));
                }
                predictedMap.put(tag, p_dou);
            }

            Double prediceteTag = Double.valueOf(0);
            prediceteTag = RedisUtils.toDecimal4(1 / (1 + Math.exp(0 - predictedMap.get(tag))));
            if (prediceteTag > 0.55) {
                resultMap.put(tag, prediceteTag);
            }
        }
        return resultMap;
    }

    /**
     * 根据分词得到的内容, 取得各个分词,及其对应的出现的次数
     * @param resultContent
     * @return
     */
    public static Map<String, Integer> getFeatureMapFromContent(String resultContent) {
        Map<String, Integer> resultMap = Maps.newHashMap();
        String[] spliterStr = resultContent.split("\\|");

        for (String str : spliterStr) {
            String[] strSplit = str.split("\\,");
            for (String str2 : strSplit) {
                String[] str2Split = str2.split("\\=");
                if (str2Split.length != 2) {
                    continue;
                } else {
                    if (resultMap.containsKey(str2Split[0])) {
                        resultMap.put(str2Split[0], (resultMap.get(str2Split[0]) + Integer.parseInt(str2Split[1])));
                    } else {
                        resultMap.put(str2Split[0], Integer.parseInt(str2Split[1]));
                    }
                }
            }
        }
        return resultMap;

    }

    public static void main(String[] args) {
    	String url = "http://localhost:8081/AxisWS/asia.wildfire.Featurer";
		Endpoint.publish(url, new Featurer());
		Featurer.initFeatureMap();
    }

}
