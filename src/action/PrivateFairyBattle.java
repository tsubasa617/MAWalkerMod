package action;

import info.BattleDB;
import info.FairyBattleInfo;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Go;
import walker.Info;
import walker.Process;
import walker.Go.LogType;

public class PrivateFairyBattle {
	public static final Action Name = Action.PRIVATE_FAIRY_BATTLE;
	private static final String URL_PRIVATE_BATTLE = "http://web.million-arthurs.com/connect/app/private_fairy/private_fairy_battle?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		boolean result = false;
		if (BattleDB.notBattleSortedSet.size() <= 0) {
			return result;
		}

		if (BattleDB.notBattleSortedSet.isEmpty() == false) {
			FairyBattleInfo fbi = BattleDB.notBattleSortedSet.first();
			result = post(fbi.deckNo, fbi.serialId, fbi.discoverer_id);

			if (result) {
				Go.log("戰鬥 " + fbi.toString(), LogType.INFO);

				BattleDB.notBattleSortedSet.remove(fbi);
				BattleDB.hadBattleArrayList.add(fbi);
			}
		}

		return result;
	}

	private static boolean post(String no, String serialId, String discoverer_id)
			throws Exception {
		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();

		post.add(new BasicNameValuePair("no", no));
		post.add(new BasicNameValuePair("serial_id", serialId));
		post.add(new BasicNameValuePair("user_id", discoverer_id));
		try {
			response = Process.network.ConnectToServer(URL_PRIVATE_BATTLE,
					post, false);
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
			ErrorData.currentErrorType = ErrorData.ErrorType.PrivateFairyBattleDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.PrivateFairyBattleResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			if (Process.info.LatestFairyList.size() > 1000) {
				Process.info.LatestFairyList.poll();
			}
			Process.info.LatestFairyList.offer(Process.info.fairy);

			if (((Boolean) xpath.evaluate("count(//private_fairy_top) > 0",
					doc, XPathConstants.BOOLEAN)).booleanValue()) {
				Process.info.events.push(Info.EventType.recvPFBGood);
				Process.info.events.push(Info.EventType.fairyBattleEnd);

				return true;
			}

			ParseUserDataInfo.parse(doc);
			ParseCardList.parse(doc);
			if (xpath.evaluate("//battle_result/winner", doc).equals("1")) {
				Process.info.events.push(Info.EventType.fairyBattleWin);

				if (Process.info.userId.equals(discoverer_id)) {
					Process.info.ownFairyKilled = true;
				}
			} else {
				Process.info.events.push(Info.EventType.recvPFBGood);
				Process.info.events.push(Info.EventType.fairyBattleLose);
			}

			Process.info.SetTimeoutByAction(Name);

			String spec = xpath
					.evaluate(
							"//private_fairy_reward_list/special_item/after_count",
							doc);
			if (spec.length() != 0)
				Process.info.gather = Integer.parseInt(spec);
			else {
				Process.info.gather = -1;
			}

			// 检查觉醒
			if (((Boolean) xpath.evaluate("count(//ex_fairy/rare_fairy)>0",
					doc, XPathConstants.BOOLEAN)).booleanValue()) {
				Process.info.fairy.fairyType = FairyBattleInfo.PRIVATE
						| FairyBattleInfo.SELF | FairyBattleInfo.RARE;
				Process.info.fairy.fairyLevel = xpath.evaluate(
						"//ex_fairy/rare_fairy/lv", doc);
				Process.info.fairy.serialId = xpath.evaluate(
						"//ex_fairy/rare_fairy/serial_id", doc);
				Process.info.fairy.userId = xpath.evaluate(
						"//ex_fairy/rare_fairy/discoverer_id", doc);
				Process.info.events.push(Info.EventType.fairyTransform);
			}
		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.PrivateFairyBattleDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		return true;
	}
}
