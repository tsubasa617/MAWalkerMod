package action;

import java.util.ArrayList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Info;
import walker.Process;

public class GuildTop {
	public static final Action Name = Action.GUILD_TOP;
	private static final String URL_GUILD_TOP = "http://web.million-arthurs.com/connect/app/guild/guild_top?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		// check tickets
		if (Process.info.ticket <= 0) {
			return false;
		}

		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		try {
			response = Process.network.ConnectToServer(URL_GUILD_TOP, post,
					false);
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
			ErrorData.currentErrorType = ErrorData.ErrorType.GuildTopDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.GuildTopResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			if (GuildDefeat.judge(doc)) {
				Process.info.events.push(Info.EventType.guildTopRetry);
				return false;
			}

			if (((Boolean) xpath.evaluate("count(//guild_top_no_fairy)>0", doc,
					XPathConstants.BOOLEAN)).booleanValue()) {
				Process.info.NoFairy = true;
				// Go.log("沒有外敵");
				return false;
			}

			Process.info.NoFairy = false;
			Process.info.fairy.fairyName = xpath.evaluate("//fairy/name", doc);
			Process.info.fairy.serialId = xpath.evaluate("//fairy/serial_id",
					doc);
			Process.info.fairy.guildId = xpath.evaluate(
					"//fairy/discoverer_id", doc);
			Process.info.fairy.fairyLevel = xpath.evaluate("//fairy/lv", doc);
			Process.info.fairy.hp = xpath.evaluate("//fairy/hp", doc);
			Process.info.fairy.hp_max = xpath.evaluate("//fairy/hp_max", doc);
			Process.info.fairy.Spp = xpath.evaluate("//spp_skill_effect", doc);// 使用BC3％回復

			Process.info.events.push(Info.EventType.guildBattle);
			return true;
		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.GuildTopDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}
	}
}
