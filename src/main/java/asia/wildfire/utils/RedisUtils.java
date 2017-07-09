package asia.wildfire.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.trigonic.jedis.NamespaceJedis;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author liuxiaochen
 * 
 */
public class RedisUtils {

    private final static String REDIS_IP = "54.248.232.237";
    private final static Integer REDIS_PORT = 6379;
    private final static String REDIS_PASSWORD = "";
    private static NamespaceJedis jedis = null;
    static {
        jedis = initJedis();
        getTagList();
    }



	public static void put(String key, List<String> list) {
		Jedis jedis = initJedis();// new Jedis(config.getIp(), Integer.parseInt(config.getPort()));
		String[] s = new String[list.size()];
		for (int i = 0; i < list.size(); i++) { 
			s[i] = list.get(i);
		}
		jedis.rpush(key, s);
		jedis.disconnect();
	}


	public static void put(String key, String value) {
		Jedis jedis = initJedis();
		jedis.set(key, value);

		jedis.disconnect();
	}

	public static List<String> getValue(String key, long start, long end) {
		Jedis jedis = initJedis();

		List<String> list = jedis.lrange(key, start, end);

		jedis.disconnect();

		return list;
	}


	public long getLiveTime(String key) {
		Jedis jedis = initJedis();

		long time = 0;
		time = jedis.ttl(key);

		jedis.disconnect();

		return time;
	}


	public static void setLiveTime(String key, int time) {
		Jedis jedis = initJedis();
		jedis.expire(key, time);

		jedis.disconnect();
	}


	public static void delKey(String key) {
		Jedis jedis = initJedis();
		jedis.del(key);
		jedis.disconnect();

	}

	public static long delRedisKey(String key) {
		Jedis jedis = initJedis();
		long i = jedis.del(key);
		jedis.disconnect();
		return i;
	}


	public static void hmSet(String key, Map<String, String> map) {
		Jedis jedis = initJedis();
		jedis.hmset(key, map);

		jedis.disconnect();
	}


	public static String hGet(String key, String mapKey) {
		Jedis jedis = initJedis();

		String str = "";
		str = jedis.hget(key, mapKey);

		jedis.disconnect();

		return str;
	}

	public static String getStringValue(String key) {
		Jedis jedis = initJedis();

		String str = jedis.get(key) == null ? "" : jedis.get(key);

		jedis.disconnect();

		return str;
	}


	public static boolean keyExist(String key) {
		Jedis jedis = initJedis();

		boolean flag = jedis.exists(key);

		jedis.disconnect();

		return flag;
	}


	public static long getKeyCount(String key) {
		Jedis jedis = initJedis();

		long length = 0;
		length = jedis.llen(key);

		jedis.disconnect();

		return length;
	}


	public static NamespaceJedis initJedis(){
        Jedis js = new Jedis(REDIS_IP, REDIS_PORT, 60000);
        NamespaceJedis jedis = new NamespaceJedis("parameters", js);
		return jedis;
	}


    public static List<String> getTagList() {
        List<String> tagList = Lists.newArrayList();
        tagList = jedis.lrange("tag_list", 0 , jedis.llen("tag_list"));
//        System.out.println(resultList);
        return tagList;
    }


    public static Map<String, Map<String, Double>> getLikelihoodValueMap() {
        Map<String, Map<String, Double>> linkihoodMap = Maps.newHashMap();
        System.out.println("begin select Likelihood will waste some time");
        String str = jedis.hget("parameters","likelihood");
        JSONObject jo = null;
        try {
            jo = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray name_arr = jo.names();
        int len = name_arr.length();
        Map<String, Double> reValuMap = null;
        for (int j = 0; j < len; j++) {
            reValuMap = Maps.newHashMap();
            try {
                JSONObject this_jo = jo.getJSONObject(name_arr.getString(j));
                JSONArray name_arr2 = this_jo.names();
                int name_arr2_len = name_arr2.length();

                for (int k = 0; k < name_arr2_len; k++) {
                    reValuMap.put(name_arr2.getString(k), this_jo.getDouble(name_arr2.getString(k)));
                }
                linkihoodMap.put(name_arr.getString(j), reValuMap);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        str = null;
        jo = null;
        return linkihoodMap;
    }


    public static Map<String, Double> getPriorValueMap() {
        Map<String, Double> priorMap = Maps.newHashMap();
        String str = jedis.hget("parameters","prior");
        JSONObject jo = null;
        try {
            jo = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray name_arr = jo.names();
        int len = name_arr.length();
        for (int j = 0; j < len; j++) {
            try {
                priorMap.put(name_arr.getString(j), jo.getDouble(name_arr.getString(j)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        jo = null;
        System.out.println("priorMap : " + priorMap);
        return priorMap;
    }
    public static Double toDecimal4(Double input){
        BigDecimal bd = new BigDecimal(input);
        return bd.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

	
	public static void main(String[] args) throws JSONException {
//        getLikelihood();
//        getPrior();
//        getTagList();
//		Jedis jedis = new Jedis(REDIS_IP, REDIS_PORT);
//        System.out.println(jedis);
//        jedis.set("namespace", "1");
//        System.out.println(jedis.exists("tag_list"));
//        System.out.println();

//        Jedis js = new Jedis(REDIS_IP, REDIS_PORT);
//        NamespaceJedis jedis = new NamespaceJedis("parameters", js);
//        Jedis jedis = initJedis();
//
//        System.out.println(jedis.lrange("tag_list", 0, 20));
        System.out.println(toDecimal4(Double.parseDouble("2.053493032598677E-4")));
        System.out.println(toDecimal4(Double.parseDouble("2.053493032598677")));
    }
}
