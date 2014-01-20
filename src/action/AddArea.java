package action;

import info.Area;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import walker.ErrorData;
import walker.Go;
import walker.Go.LogType;
import walker.Info;
import walker.Process;

public class AddArea {
	public static final Action Name = Action.ADD_AREA;
	private static final String URL_AREA = "http://web.million-arthurs.com/connect/app/exploration/area?cyt=1";
	public static final int MAP_NO = 100000;
	private static byte[] response;

	/**
	 * 逻辑：<br/>
	 * 1.若跑日常图，进度不为100%时跑日常图<br/>
	 * 2.若初始里图编号大于10万，分表里图存储；否则全存到表图<br/>
	 * 3.获取area信息
	 */
	public static boolean run() throws Exception {
		response = null;
		try {
			response = Process.network.ConnectToServer(URL_AREA,
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
			ErrorData.currentErrorType = ErrorData.ErrorType.AreaDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.AreaResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			int areaCount = ((NodeList) xpath.evaluate(
					"//area_info_list/area_info", doc, XPathConstants.NODESET))
					.getLength();

			// area按编号从小到大排序
			for (int i = areaCount; i > 0; i--) {
				Area a = new Area();
				String p = String.format("//area_info_list/area_info[%d]/", i);
				a.areaId = Integer.parseInt(xpath.evaluate(p + "id", doc));

				if (Process.info.area.containsKey(a.areaId) == false) {
					a.areaName = xpath.evaluate(p + "name", doc);
					a.exploreProgress = Integer.parseInt(xpath.evaluate(p
							+ "prog_area", doc));

					// 输出地图信息
					Go.log("地圖：" + a.areaName + "，編號：" + a.areaId, LogType.INFO);

					// 跑日常图
					if (Info.DailyMap == true
							&& Info.DailyMapNo.contains(Integer
									.valueOf(a.areaId)) == true
							&& a.exploreProgress < 100) {
						Process.info.area.put(a.areaId, a);
					}

					// 跑活动图
					if (a.areaId > MAP_NO) {
						// 判断第一张里图编号是否有效
						if (Info.firstInnerMapNo > MAP_NO) {
							// 里图 or 表图
							if (a.areaId >= Info.firstInnerMapNo) {
								Process.info.areaInner.put(a.areaId, a);
							} else {
								Process.info.area.put(a.areaId, a);
							}
						} else {
							Process.info.area.put(a.areaId, a);
						}
					}
				}
			}// end for

			return areaCount > 0;
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		}

		return false;
	}
}
