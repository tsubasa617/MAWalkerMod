package action;

import info.PartyInfo;
import info.PlayerRank;

import java.util.ArrayList;
import java.util.Collections;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import walker.ErrorData;
import walker.Go;
import walker.Process;
import walker.Go.LogType;

public class GetPartyRank {
	public static final Action Name = Action.GET_PARTY_RANK;

	// private static final String URL_GUIDE_MEMBER_LIST =
	// "http://web.million-arthurs.com/connect/app/guild/guild_member_list?cyt=1";
	private static final String URL_PARTY_ONLY_MEMBER_LIST = "http://web.million-arthurs.com/connect/app/party/party_only_member_list?cyt=1";
	private static final String URL_GUILD_INFO = "http://web.million-arthurs.com/connect/app/guild/guild_info?cyt=1";
	private static final String URL_RANKING_NEXT = "http://web.million-arthurs.com/connect/app/ranking/ranking_next?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		Document doc;
		Boolean set = false;

		try {
			response = Process.network.ConnectToServer(URL_GUILD_INFO,
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
			ErrorData.currentErrorType = ErrorData.ErrorType.PartyRankDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			set = parse(doc);
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		}

		return set;
	}

	private static boolean parse(Document doc) throws Exception {

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.FairyHistoryResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			PartyInfo.user_rank = xpath.evaluate("//guild_info/user_rank", doc);
			PartyInfo.user_hunting_point = xpath.evaluate(
					"//guild_info/user_hunting_point", doc);
			PartyInfo.own_guild_point = xpath.evaluate(
					"//guild_info/guild_hunting_point/own_guild_point", doc);
			PartyInfo.own_guild_dairy_point = xpath.evaluate(
					"//guild_info/guild_hunting_point/own_guild_dairy_point",
					doc);
			PartyInfo.own_guild_rank = xpath.evaluate(
					"//guild_info/own_guild_rank", doc);
			PartyInfo.rival_guild_point = xpath.evaluate(
					"//guild_info/guild_hunting_point/rival_guild_point", doc);
			PartyInfo.rival_guild_dairy_point = xpath.evaluate(
					"//guild_info/guild_hunting_point/rival_guild_dairy_point",
					doc);
			PartyInfo.rival_guild_rank = xpath.evaluate(
					"//guild_info/rival_guild_rank", doc);

			run2(Process.info.guildId);

			Process.info.SetTimeoutByAction(Name);

		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.FairyHistoryDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}
		return true;
	}

	public static boolean run2(String guildId) throws Exception {
		Document doc;
		try {
			ArrayList<NameValuePair> al = new ArrayList<NameValuePair>();
			al.add(new BasicNameValuePair("pid", guildId));
			response = Process.network.ConnectToServer(
					URL_PARTY_ONLY_MEMBER_LIST, al, false);
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
				return false;
			}

			NodeList fairyuser = (NodeList) xpath.evaluate(
					"//party_member_list/member", doc, XPathConstants.NODESET);
			int num = 0;
			PartyInfo.ranklist.clear();
			for (int i = 0; i < fairyuser.getLength(); i++) {
				Node f = fairyuser.item(i).getFirstChild();
				PlayerRank item = new PlayerRank();
				int iUser = 0;
				do {
					if (f.getNodeName().equals("id")) {
						iUser = Integer.parseInt(f.getFirstChild()
								.getNodeValue());
					} else if (f.getNodeName().equals("name")) {
						item.name = f.getFirstChild().getNodeValue();
					} else if (f.getNodeName().equals("guild_hunting_point")) {
						num += item.point = Integer.parseInt(f.getFirstChild()
								.getNodeValue());
					} else if (f.getNodeName().equals("town_level")) {
						item.ilv = Integer.parseInt(f.getFirstChild()
								.getNodeValue());
					} else if (f.getNodeName().equals("last_login")) {
						item.logintime = f.getFirstChild().getNodeValue();
					}
					f = f.getNextSibling();
				} while (f != null);
				item.irank = geturerrank(iUser);
				PartyInfo.ranklist.add(item);
			}

			String str2 = String.format("團貢：%d/%s，日貢：%s，團排：%s", num,
					PartyInfo.own_guild_point, PartyInfo.own_guild_dairy_point,
					PartyInfo.own_guild_rank);
			Go.log(str2, LogType.INFO);

			Collections.sort(PartyInfo.ranklist);
			for (PlayerRank playerrank2 : PartyInfo.ranklist) {
				Go.log(playerrank2.toString(), LogType.INFO);
			}

		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		}

		return false;
	}

	public static int geturerrank(int iUser) throws Exception {
		Document doc;
		try {
			ArrayList<NameValuePair> al = new ArrayList<NameValuePair>();
			al.add(new BasicNameValuePair("from", String.valueOf(iUser)));
			al.add(new BasicNameValuePair("ranktype_id", "4"));
			response = Process.network.ConnectToServer(URL_RANKING_NEXT, al,
					false);
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

			return Integer.parseInt(xpath.evaluate("//user_list/user[1]/rank",
					doc)) - 1;
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		}

		return 0;
	}
}
