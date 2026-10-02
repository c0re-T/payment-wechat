package com.ittxf.payment.common.util;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * http请求客户端
 *
 * <p>对 Apache HttpClient 的轻量封装，一个实例对应一次请求：
 * 构造时传入 url，调用 {@link #get()} / {@link #post()} / {@link #put()}
 * 发起请求，再通过 {@link #getStatusCode()} 与 {@link #getContent()} 取结果。</p>
 *
 * <p>主要用于微信支付 APIv2 的 XML 报文交互（v2 不走签名装饰器）。
 * 注意本类持有 url、param 等可变状态，<b>线程不安全</b>，不要作为单例 Bean 复用。</p>
 */
public class HttpClientUtils {
	/**
	 * 请求地址
	 */
	private String url;
	/**
	 * 表单参数，GET 时拼接到 query string，POST/PUT 时作为表单体提交
	 */
	private Map<String, String> param;
	/**
	 * 响应状态码，请求执行完毕后回填
	 */
	private int statusCode;
	/**
	 * 响应体文本，请求执行完毕后回填
	 */
	private String content;
	/**
	 * XML 报文体，APIv2 交互时使用，设置后优先于 param 作为请求体
	 */
	private String xmlParam;
	/**
	 * 是否走 HTTPS，为 true 时会使用信任所有证书的 SSL 上下文
	 */
	private boolean isHttps;

	/**
	 * 是否按 HTTPS 方式发起请求
	 *
	 * @return true 表示使用 HTTPS
	 */
	public boolean isHttps() {
		return isHttps;
	}

	/**
	 * 设置是否走 HTTPS
	 *
	 * @param isHttps true 时启用 HTTPS
	 */
	public void setHttps(boolean isHttps) {
		this.isHttps = isHttps;
	}

	/**
	 * 获取当前设置的 XML 报文体
	 *
	 * @return XML 字符串，未设置时为 null
	 */
	public String getXmlParam() {
		return xmlParam;
	}

	/**
	 * 设置 XML 报文体，用于 APIv2 等基于 XML 的接口
	 *
	 * @param xmlParam 完整的 XML 请求报文
	 */
	public void setXmlParam(String xmlParam) {
		this.xmlParam = xmlParam;
	}

	/**
	 * 构造请求客户端，同时指定地址与参数
	 *
	 * @param url   请求地址
	 * @param param 请求参数
	 */
	public HttpClientUtils(String url, Map<String, String> param) {
		this.url = url;
		this.param = param;
	}

	/**
	 * 构造请求客户端，仅指定地址，参数后续通过 addParameter 追加
	 *
	 * @param url 请求地址
	 */
	public HttpClientUtils(String url) {
		this.url = url;
	}

	/**
	 * 整体替换请求参数
	 *
	 * @param map 新的参数集合
	 */
	public void setParameter(Map<String, String> map) {
		param = map;
	}

	/**
	 * 追加单个请求参数，参数集合尚未初始化时自动创建
	 *
	 * @param key   参数名
	 * @param value 参数值
	 */
	public void addParameter(String key, String value) {
		if (param == null)
			param = new HashMap<String, String>();
		param.put(key, value);
	}

	/**
	 * 发起 POST 请求，参数以表单或 XML 形式放在请求体中
	 *
	 * @throws ClientProtocolException 协议不兼容时抛出
	 * @throws IOException             网络读写异常时抛出
	 */
	public void post() throws ClientProtocolException, IOException {
		HttpPost http = new HttpPost(url);
		setEntity(http);
		execute(http);
	}

	/**
	 * 发起 PUT 请求，参数设置方式与 POST 一致
	 *
	 * @throws ClientProtocolException 协议不兼容时抛出
	 * @throws IOException             网络读写异常时抛出
	 */
	public void put() throws ClientProtocolException, IOException {
		HttpPut http = new HttpPut(url);
		setEntity(http);
		execute(http);
	}

	/**
	 * 发起 GET 请求，参数以 query string 形式拼到 url 后面
	 *
	 * <p>注意：参数值未做 URL 编码，含中文或特殊字符时可能出错，
	 * 这种场景应改用 POST，或自行编码后再传入。</p>
	 *
	 * @throws ClientProtocolException 协议不兼容时抛出
	 * @throws IOException             网络读写异常时抛出
	 */
	public void get() throws ClientProtocolException, IOException {
		if (param != null) {
			StringBuilder url = new StringBuilder(this.url);
			boolean isFirst = true;
			for (String key : param.keySet()) {
				if (isFirst) {
					url.append("?");
					isFirst = false;
				}else {
					url.append("&");
				}
				url.append(key).append("=").append(param.get(key));
			}
			this.url = url.toString();
		}
		HttpGet http = new HttpGet(url);
		execute(http);
	}

	/**
	 * set http post,put param
	 *
	 * <p>为 POST/PUT 请求装配请求体：param 转成表单实体，xmlParam 转成字符串实体，
	 * 两者同时存在时 xmlParam 会后写入并覆盖表单。</p>
	 *
	 * @param http 支持携带请求体的 POST 或 PUT 请求
	 */
	private void setEntity(HttpEntityEnclosingRequestBase http) {
		if (param != null) {
			List<NameValuePair> nvps = new LinkedList<NameValuePair>();
			for (String key : param.keySet())
				nvps.add(new BasicNameValuePair(key, param.get(key))); // 参数
			http.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8)); // 设置参数
		}
		if (xmlParam != null) {
			http.setEntity(new StringEntity(xmlParam, Consts.UTF_8));
		}
	}

	/**
	 * 执行请求并回填状态码与响应体
	 *
	 * <p>isHttps 为 true 时构造信任所有证书的 SSL 上下文，仅适用于联调自签名服务，
	 * 生产环境会削弱证书校验，存在中间人攻击风险。</p>
	 *
	 * <p>注意：本方法内部吞掉所有异常只打印堆栈，调用方无法感知请求是否真的成功，
	 * 需自行结合 {@link #getStatusCode()} 判断。</p>
	 *
	 * @param http 待执行的请求
	 * @throws ClientProtocolException 协议不兼容时抛出
	 * @throws IOException             关闭客户端连接异常时抛出
	 */
	private void execute(HttpUriRequest http) throws ClientProtocolException,
			IOException {
		CloseableHttpClient httpClient = null;
		try {
			if (isHttps) {
				SSLContext sslContext = new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustStrategy() {
							// 信任所有
							public boolean isTrusted(X509Certificate[] chain,
									String authType)
									throws CertificateException {
								return true;
							}
						}).build();
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
						sslContext);
				// 使用跳过证书校验的工厂构建客户端
				httpClient = HttpClients.custom().setSSLSocketFactory(sslsf)
						.build();
			} else {
				httpClient = HttpClients.createDefault();
			}
			CloseableHttpResponse response = httpClient.execute(http);
			try {
				if (response != null) {
					if (response.getStatusLine() != null)
						statusCode = response.getStatusLine().getStatusCode();
					HttpEntity entity = response.getEntity();
					// 响应内容
					content = EntityUtils.toString(entity, Consts.UTF_8);
				}
			} finally {
				response.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpClient.close();
		}
	}

	/**
	 * 获取响应状态码
	 *
	 * @return HTTP 状态码；请求未执行或执行失败时为 0
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * 获取响应体文本
	 *
	 * @return 响应内容；请求未执行或执行失败时为 null
	 * @throws ParseException 解析响应内容异常时抛出
	 * @throws IOException    读取响应内容异常时抛出
	 */
	public String getContent() throws ParseException, IOException {
		return content;
	}

}
