package action;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class GetFairyReward {
	public static final Action Name = Action.GET_FAIRY_REWARD;
	private static final String URL_GET_FAIRY_REWARD = "http://web.million-arthurs.com/connect/app/private_fairy/private_fairy_rewards?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		if (Process.info.lastReceiveRewardTime + 60 * 1000 > System
				.currentTimeMillis()) {
			// 距上次领取不足1分钟，不发起请求
			return false;
		}

		try {
			response = Process.network.ConnectToServer(URL_GET_FAIRY_REWARD,
					new ArrayList<NameValuePair>(), false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		Document doc;
		try {
			doc = Process.ParseXMLBytes(response, Name.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GetFairyRewardDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			if (!xpath.evaluate("/response/header/error/code", doc).equals(
					"1010")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.GetFairyRewardResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			ErrorData.text = xpath.evaluate("/response/header/error/message",
					doc);

			// update time
			Process.info.lastReceiveRewardTime = System.currentTimeMillis();

			// 收箱子物品
			if (Info.AutoReceiveBox == true) {
				GetRewardBox.list();
				if (Process.info.boxList.size() > 600) {
					GetRewardBox.listRewardbox();
				}
			}

			return true;
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		}

		return false;
	}
}
