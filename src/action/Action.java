package action;

public enum Action {
	NOTHING, // 什么都不做
	LOGIN, // 登录
	GET_FLOOR_INFO, // 将会刷新area和floor
	ADD_AREA, // 添加区域信息，内部使用
	GOTO_FLOOR, // 进入某floor，快速取得AP，BC以及经验值和物品等信息
	GET_FAIRY_LIST, // 获取妖精列表
	PRIVATE_FAIRY_BATTLE, // 妖精战斗
	EXPLORE, // 探索
	GET_FAIRY_REWARD, // 获取妖精战奖励
	GUILD_TOP, GUILD_BATTLE, // 守卫战相关
	SELL_CARD, // 卖卡
	LV_UP, // 升级
	PFB_GOOD, RECV_PFB_GOOD, // 给赞收赞
	USE, // 自动吃药
	GET_REWARD_BOX, // 获取箱子物品
	GET_RANK, // 获取个人排名信息
	GET_PARTY_RANK, // 获取团队排名信息
	DROP_FRIENDSHIP_POINT// 消耗伴点
}
