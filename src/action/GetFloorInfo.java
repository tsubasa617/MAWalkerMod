package action;

import info.Area;
import info.Floor;
import java.util.ArrayList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import walker.ErrorData;
import walker.Info;
import walker.Process;

public class GetFloorInfo {
	public static final Action Name = Action.GET_FLOOR_INFO;
	private static final String URL_FLOOR = "http://web.million-arthurs.com/connect/app/exploration/floor?cyt=1";
	private static byte[] response;

	/**
	 * 若选择跑固定图则读取固定图信息，忽略下面的步骤<br/>
	 * <br/>
	 * 处理逻辑：<br/>
	 * 1.有活动图只走活动图<br/>
	 * 2.优先里图？“是”到3，“否”到<br/>
	 * 3.里图设置最低cost或所有图全清，则走最低cost；否则正常跑图<br/>
	 * 4.floor集合是空的？“是”到5，“否”结束<br/>
	 * 5.只走里图？“是”结束，“否”到6<br/>
	 * 6.表图设置最低cost或所有图全清，则走最低cost；否则正常跑图
	 * */
	public static boolean run() throws Exception {
		try {
			// 一些初始化工作
			Process.info.area.clear();
			Process.info.areaInner.clear();
			Process.info.floor.clear();
			Process.info.front = null;
			Process.info.AllClear = true;

			if (AddArea.run() == true) {
				// 跑固定图
				if (Info.OneMapOnly == true) {
					Area area = Process.info.area.get(Info.OneMapId) != null ? Process.info.area
							.get(Info.OneMapId) : Process.info.areaInner
							.get(Info.OneMapId);
					Process.info.front = getFloor(area, GetFloorType.Specified,
							Info.OneMapCost);
					return true;
				}

				// 日常图
				for (Integer no : Info.DailyMapNo) {
					if (Process.info.area.containsKey(no)) {
						Process.info.front = getFloor(
								Process.info.area.get(no), GetFloorType.Last,
								null);
						return true;
					}
				}

				// 优先里图
				if (Info.InnerMapFirst == true) {
					if (Process.info.areaInner.isEmpty() == false) {
						// 最低AP图
						if (Info.MinAPOnlyInner == true) {
							Process.info.front = getFloor(
									Process.info.areaInner.firstEntry()
											.getValue(), GetFloorType.First,
									null);
							return true;
						}

						// 正常走图
						Process.info.front = getFloor(Process.info.areaInner
								.lastEntry().getValue(), GetFloorType.Last,
								null);

						// 检测是否跑完图
						if (Process.info.AllClear == true) {
							Process.info.front = getFloor(
									Process.info.areaInner.firstEntry()
											.getValue(), GetFloorType.First,
									null);
						}
						return true;
					}
				}

				// 只走里图
				if (Info.InnerMapOnly == true) {
					return Process.info.front != null;
				}

				// 表图
				// 正常走图
				if (Info.MinAPOnly == false) {
					Process.info.front = getFloor(Process.info.area.lastEntry()
							.getValue(), GetFloorType.Last, null);

					// 检测是否跑完全图
					if (Process.info.AllClear == false) {
						return true;
					}
				}

				// 跑最低cost图
				for (Area area : Process.info.area.values()) {
					getFloor(area, GetFloorType.ALL, null);
				}
				Process.info.front = Process.info.floor.firstEntry().getValue();

				return true;
			}
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
				throw ex;
			}
		} finally {
			Process.info.SetTimeoutByAction(Name);
			Process.info.lastUpdateTime = System.currentTimeMillis();
		}

		return false;
	}

	private enum GetFloorType {
		ALL, First, Last, Specified
	}

	private static Floor getFloor(Area a, GetFloorType type, Integer cost)
			throws Exception {
		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("area_id", String.valueOf(a.areaId)));

		try {
			response = Process.network.ConnectToServer(URL_FLOOR, post, false);
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
			ErrorData.currentErrorType = ErrorData.ErrorType.AreaDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		int floorCount = ((NodeList) xpath.evaluate(
				"//floor_info_list/floor_info", doc, XPathConstants.NODESET))
				.getLength();
		String aid = xpath.evaluate("//exploration_floor/area_id", doc);

		switch (type) {
		case ALL:
			// 从最高层往下遍历
			for (int j = floorCount; j > 0; j--) {
				Floor f = new Floor();
				String p = String
						.format("//floor_info_list/floor_info[%d]/", j);
				f.areaId = aid;
				f.floorId = xpath.evaluate(p + "id", doc);
				f.cost = Integer.parseInt(xpath.evaluate(p + "cost", doc));
				f.progress = Integer.parseInt(xpath.evaluate(p + "progress",
						doc));
				f.type = xpath.evaluate(p + "type", doc);

				// 如果该cost的图已存在列表中
				if (Process.info.floor.containsKey(f.cost)) {
					// 如果已有的编号大于等于该图，则跳过
					if (Integer
							.parseInt((Process.info.floor.get(f.cost)).areaId) >= Integer
							.parseInt(f.areaId)) {
						continue;
					}
				}

				Process.info.floor.put(f.cost, f);
			}

			break;
		case First:
			Floor f = new Floor();
			String p = String.format("//floor_info_list/floor_info[%d]/", 1);
			f.areaId = aid;
			f.floorId = xpath.evaluate(p + "id", doc);
			f.cost = Integer.parseInt(xpath.evaluate(p + "cost", doc));
			f.progress = Integer.parseInt(xpath.evaluate(p + "progress", doc));
			f.type = xpath.evaluate(p + "type", doc);

			return f;
		case Last:
			Floor f1 = new Floor();
			String p1 = String.format("//floor_info_list/floor_info[%d]/",
					floorCount);
			f1.areaId = aid;
			f1.floorId = xpath.evaluate(p1 + "id", doc);
			f1.cost = Integer.parseInt(xpath.evaluate(p1 + "cost", doc));
			f1.progress = Integer
					.parseInt(xpath.evaluate(p1 + "progress", doc));
			f1.type = xpath.evaluate(p1 + "type", doc);

			if (a.exploreProgress < 100) {
				Process.info.AllClear = false;
			}

			return f1;
		case Specified:
			for (int j = 1; j <= floorCount; j++) {
				String s = String
						.format("//floor_info_list/floor_info[%d]/", j);
				int cc = Integer.parseInt(xpath.evaluate(s + "cost", doc));

				if (cc == cost) {
					Floor f2 = new Floor();
					f2.areaId = aid;
					f2.floorId = xpath.evaluate(s + "id", doc);
					f2.cost = cc;
					f2.progress = Integer.parseInt(xpath.evaluate(s
							+ "progress", doc));
					f2.type = xpath.evaluate(s + "type", doc);

					return f2;
				}
			}

			break;
		}

		return null;
	}
}
