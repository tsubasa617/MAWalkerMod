package action;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Go;
import walker.Info;
import walker.Process;
import walker.Go.LogType;

public class DropFriendshipPoint {
	private static final Action NAME = Action.DROP_FRIENDSHIP_POINT;
	private static final String URL = "http://web.million-arthurs.com/connect/app/gacha/buy?cyt=1";
	private static byte[] response;

	public static int run() throws Exception {
		Document doc;
		try {
			ArrayList<NameValuePair> al = new ArrayList<NameValuePair>();
			al.add(new BasicNameValuePair("auto_build", "1"));
			al.add(new BasicNameValuePair("bulk", "1"));
			al.add(new BasicNameValuePair("product_id", "1"));

			response = Process.network.ConnectToServer(URL, al, false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		try {
			doc = Process.ParseXMLBytes(response, NAME.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.PFB_GoodDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.FairyHistoryResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return 0;
			}

			Process.info.friendshipPoint = Integer.parseInt(xpath.evaluate(
					"//your_data/friendship_point", doc));
			ParseCardList.parse(doc);

			// 卖卡
			if (Info.AutoSellCard == true) {
				if (SellCard.cardsToSell() == true) {
					SellCard.run();
					Go.log(ErrorData.text, LogType.INFO);
					ErrorData.text = "";
				}
			}

			// 判断卡满
			if (Process.info.cardList.size() + 10 > Process.info.cardMax) {
				return 0;
			}

			return Process.info.friendshipPoint;
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		}

		return 0;
	}
}
