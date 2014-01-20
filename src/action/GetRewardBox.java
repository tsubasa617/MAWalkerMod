package action;

import info.Box;
import info.Card;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import walker.ErrorData;
import walker.Go;
import walker.Info;
import walker.Process;
import walker.Go.LogType;

public class GetRewardBox {
	public static final Action Name = Action.GET_REWARD_BOX;
	private static final String URL_LIST_REWARD_BOX = "http://web.million-arthurs.com/connect/app/menu/rewardbox?cyt=1";
	private static final String URL_GET_REWARD_BOX = "http://web.million-arthurs.com/connect/app/menu/get_rewards?cyt=1";

	private static byte[] response;

	public static void listRewardbox() throws Exception {
		list();
		Go.log(String.format("箱子裏有[%d]個物品。", Process.info.boxList.size()),
				LogType.INFO);

		do {
			for (int i = 0; i < Process.info.boxList.size(); i++) {
				if (Process.info.boxList.get(i).exist == false) {
					Process.info.boxList.remove(i);
				}
			}

			if (Process.info.boxList.size() == 0) {
				break;
			}

			getAndSell();

			if (Process.info.cardList.size() == Card.MAX) {
				break;
			}
		} while (true);
	}

	public static void list() throws Exception {
		Document doc;
		try {
			response = Process.network.ConnectToServer(URL_LIST_REWARD_BOX,
					new ArrayList<NameValuePair>(), false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		try {
			doc = Process.ParseXMLBytes(response, Name.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GetRewardBoxDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		parse(doc);
	}

	public static void parse(Document doc) throws NumberFormatException,
			XPathExpressionException {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		int boxCount = ((NodeList) xpath.evaluate("//rewardbox_list/rewardbox",
				doc, XPathConstants.NODESET)).getLength();

		Process.info.boxList = new ArrayList<Box>();
		for (int i = 1; i <= boxCount; i++) {
			Box b = new Box();
			String p = String.format("//rewardbox_list/rewardbox[%d]", i);

			b.boxId = xpath.evaluate(p + "/id", doc);
			b.boxType = Integer.parseInt(xpath.evaluate(p + "/type", doc));
			b.exist = true;
			Process.info.boxList.add(b);
		}
	}

	public static void getAndSell() throws Exception {
		Document doc;
		int count = 0;
		String toGet = "";

		for (Box b : Process.info.boxList) {
			if (!b.exist) {
				continue;
			}

			if (toGet.isEmpty()) {
				toGet = b.boxId;
			} else {
				toGet += "," + b.boxId;
			}

			b.exist = false;

			if (++count >= 20) {
				break;
			}
		}

		Process.info.toGet = toGet;

		if (!toGet.isEmpty()) {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
			post.add(new BasicNameValuePair("notice_id", Process.info.toGet));

			try {
				response = Process.network.ConnectToServer(URL_GET_REWARD_BOX,
						post, false);
			} catch (Exception ex) {
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
				ErrorData.text = ex.getLocalizedMessage();
				throw ex;
			}

			try {
				doc = Process.ParseXMLBytes(response, Name.name());
			} catch (Exception ex) {
				ErrorData.currentDataType = ErrorData.DataType.bytes;
				ErrorData.currentErrorType = ErrorData.ErrorType.GetRewardBoxDataError;
				ErrorData.bytes = response;
				throw ex;
			}

			try {
				if (!xpath.evaluate("/response/header/error/code", doc).equals(
						"0")) {
					ErrorData.currentErrorType = ErrorData.ErrorType.GetRewardBoxResponse;
					ErrorData.currentDataType = ErrorData.DataType.text;
					ErrorData.text = xpath.evaluate(
							"/response/header/error/message", doc);
					Go.log(ErrorData.text, LogType.INFO);
				}
			} catch (Exception ex) {
				ErrorData.currentDataType = ErrorData.DataType.bytes;
				ErrorData.currentErrorType = ErrorData.ErrorType.GetRewardBoxDataError;
				ErrorData.bytes = response;
				throw ex;
			}

			// 卖卡
			if (Info.AutoSellCard == true) {
				ParseCardList.parse(doc);
				if (SellCard.cardsToSell() == true) {
					SellCard.run();
					Go.log(ErrorData.text.replace("\n", ""), LogType.INFO);
					ErrorData.text = "";
				}
			}
		}
	}

}
