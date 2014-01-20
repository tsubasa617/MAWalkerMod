package action;

import info.Card;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class SellCard {
	public static final Action Name = Action.SELL_CARD;
	private static final String URL_SELL_CARD = "http://web.million-arthurs.com/connect/app/trunk/sell?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		if (Process.info.toSell.isEmpty()) {
			return false;
		}

		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("serial_id", Process.info.toSell));

		try {
			response = Process.network.ConnectToServer(URL_SELL_CARD, post,
					false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getLocalizedMessage();
			throw ex;
		}

		Document doc;
		try {
			doc = Process.ParseXMLBytes(response, Name.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.SellCardDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals(
					"1010")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.GuildBattleResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			ErrorData.text = xpath.evaluate("/response/header/error/message",
					doc);
			Process.info.toSell = "";
			return true;
		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;

			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.SellCardDataError;
			ErrorData.bytes = response;
			throw ex;
		}
	}

	public static boolean cardsToSell() {
		int count = 0;
		String toSell = "";

		for (Card c : Process.info.cardList) {
			// 卡片不存在跳过
			if (c.exist == false) {
				continue;
			}

			// 大于指定价格的闪卡不卖
			if (c.holo == true && c.price > Info.MaxSellCardMoney) {
				continue;
			}

			// 大于指定价格，且等级大于1的卡不卖
			if (c.price > Info.MaxSellCardMoney && c.lv > 1) {
				continue;
			}

			// 切利
			if (c.hp <= 2
					&& c.atk <= 2
					&& Info.CanBeSold.contains(Integer.valueOf(c.cardId)) == false) {
				continue;
			}

			// 124号（狼娘）、142号（牛仔）、49号（女仆）卡不卖
			int cardId = Integer.valueOf(c.cardId);
			if (cardId == 124 || cardId == 142 || cardId == 49) {
				if (Info.CanBeSold.contains(cardId) == true && c.lv == 1) {
					// can be sold
				} else {
					continue;
				}
			}

			// 卖卡
			if (c.price <= Info.MaxSellCardMoney
					|| Info.CanBeSold.contains(Integer.valueOf(c.cardId))) {
				if (toSell.isEmpty()) {
					toSell = c.serialId;
				} else {
					toSell += "," + c.serialId;
				}

				count++;
				c.exist = false;
			}

			if (count >= 30) {
				break;
			}
		}// end for

		Process.info.toSell = toSell;

		return !toSell.isEmpty();
	}
}
