// 生成 FitCoach 论文的标题/作者/摘要/关键词等前置内容
// 按附录一模板格式排版
const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, AlignmentType, HeadingLevel,
} = require('docx');

const FONT_HEI = 'SimHei';      // 黑体
const FONT_SONG = 'SimSun';     // 宋体
const FONT_KAI = 'KaiTi';       // 楷体
const FONT_EN = 'Times New Roman';

// 字号(half-points): 三号=32, 四号=28, 小四=24, 五号=21, 小五=18
const SIZE_3 = 32;
const SIZE_4 = 28;
const SIZE_4S = 24;   // 小四
const SIZE_5 = 21;
const SIZE_5S = 18;   // 小五

// 工具:中英文混排段落
const p = (children, opts = {}) => new Paragraph({
  alignment: opts.align || AlignmentType.LEFT,
  spacing: { line: 360, after: opts.afterPt ? opts.afterPt * 20 : 80 },
  ...(opts.indent ? { indent: opts.indent } : {}),
  children,
});

const t = (text, opts = {}) => new TextRun({
  text,
  font: { name: opts.font || FONT_SONG, hint: 'eastAsia' },
  size: opts.size || SIZE_5,
  bold: !!opts.bold,
});

// 段落:中英文字体可分别指定(中文用 east-asia hint)
const tMix = (text, opts = {}) => new TextRun({
  text,
  font: opts.font || FONT_SONG,
  size: opts.size || SIZE_5,
  bold: !!opts.bold,
});

// 注释/提示(灰色,小号)
const note = (text) => new TextRun({
  text,
  font: FONT_SONG,
  color: '808080',
  size: SIZE_5S,
  italics: true,
});

const blank = () => new Paragraph({ children: [new TextRun('')] });

// =============== 内容 ===============

const children = [];

// ---------- 提示 ----------
children.push(new Paragraph({
  alignment: AlignmentType.LEFT,
  children: [note('（说明:本文档已按"附录一:研究方法与创新综合实践报告模板"的格式要求排版,你直接把对应段落复制到论文 docx 的相应位置即可。需要你补充的字段都标注为【请填写...】,空着的部分请按提示替换。）')],
}));
children.push(blank());

// ---------- 中文题目(三号) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 400, after: 200 },
  children: [tMix('面向居家老年人的智能健身陪练系统设计与实现', { font: FONT_HEI, size: SIZE_3, bold: true })],
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 400, after: 240 },
  children: [tMix('——以一级项目 FitCoach 为例', { font: FONT_HEI, size: SIZE_3, bold: true })],
}));

// ---------- 中文作者(四号宋体) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 360, after: 100 },
  children: [
    tMix('【请填写:作者姓名】', { font: FONT_SONG, size: SIZE_4 }),
  ],
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  children: [note('（单作者单位无需加上标；如为多作者多单位,在姓名右上角分别标注 1、2…对应单位编号）')],
}));

// ---------- 中文单位(小五号) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 320, after: 60 },
  children: [
    tMix('(【请填写:学院全称】, 【请填写:城市】 【请填写:邮编】)', { size: SIZE_5S }),
  ],
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  children: [note('（城市若不是省会,要补省份；如:江苏 苏州 215021）')],
}));

// ---------- 中文邮箱(小五) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 320, after: 200 },
  children: [tMix('(【请填写:作者邮箱】)', { size: SIZE_5S })],
}));

// ---------- 英文标题(Title 四号) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 360, after: 100 },
  children: [tMix('Design and Implementation of an Intelligent Home-Fitness Coaching System for Elderly Users', { font: FONT_EN, size: SIZE_4, bold: true })],
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 360, after: 200 },
  children: [tMix('— A Case Study of the FitCoach Project', { font: FONT_EN, size: SIZE_4, bold: true })],
}));

// ---------- 英文作者(五号) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 320, after: 100 },
  children: [
    tMix('【Please fill in: Author name in pinyin, e.g. Zhang San】', { font: FONT_EN, size: SIZE_5 }),
  ],
}));

// ---------- 英文单位(小五) ----------
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { line: 320, after: 60 },
  children: [
    tMix('(【Please fill in: School/College name in English】, 【City】 【Postcode】)', { font: FONT_EN, size: SIZE_5S }),
  ],
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 240 },
  children: [note('（如:School of Computer Science and Technology, Soochow University, Suzhou 215021）')],
}));

// ---------- Abstract(五号,英文 Times New Roman) ----------
const abstractEn = `To address the difficulties faced by elderly users in home-based physical exercise — including the lack of on-site guidance, difficulty in maintaining proper training rhythm, and the high operational barrier of complex digital products — this paper designs and implements an intelligent fitness coaching system named FitCoach, based on browser-side human pose recognition. Using only a standard webcam as input, the system leverages the MediaPipe Pose framework to perform body-keypoint detection directly in the browser. Joint-angle computation combined with a finite state machine is used to recognize and count seven typical training movements, including squat, forward stretch, push-up, lunge, glute bridge, plank, and jumping jack. A rule-driven multi-dimensional scoring model is further constructed along five dimensions — rhythm, stability, depth, symmetry, and completion rate — and is integrated with text-to-speech feedback, a metronome, and a post-training report to form a complete training loop that covers action selection, real-time recognition, result feedback, local saving, and conditional uploading. The system adopts a decoupled front-end and back-end architecture based on Vue 3 and Spring Boot, and combines IndexedDB with a Progressive Web Application (PWA) to provide offline-first data persistence and installable deployment. Functional verification shows that the system can stably complete the entire training workflow under normal home-camera conditions, and is able to preserve training records under weak-network or unauthenticated states. The system features a low deployment barrier, intuitive feedback and good interpretability, providing a practical engineering solution for elderly home-exercise scenarios.`;

children.push(new Paragraph({
  alignment: AlignmentType.JUSTIFIED,
  spacing: { line: 320, after: 80 },
  children: [
    tMix('Abstract  ', { font: FONT_EN, size: SIZE_5, bold: true }),
    tMix(abstractEn, { font: FONT_EN, size: SIZE_5 }),
  ],
}));

// ---------- Key words(五号) ----------
children.push(new Paragraph({
  alignment: AlignmentType.JUSTIFIED,
  spacing: { line: 320, after: 200 },
  children: [
    tMix('Key words  ', { font: FONT_EN, size: SIZE_5, bold: true }),
    tMix('pose recognition; home fitness; elderly users; progressive web application; offline-first; multi-dimensional scoring', { font: FONT_EN, size: SIZE_5 }),
  ],
}));

// ---------- 中文摘要(五号楷体) ----------
const abstractZh = `针对居家老年人在日常锻炼中缺少现场指导、训练节奏难以把握、复杂数字产品操作门槛较高等问题,本文设计并实现了一个基于浏览器端姿态识别的智能健身陪练系统 FitCoach。系统以普通摄像头为输入,利用 MediaPipe Pose 框架在浏览器端完成人体关键点检测;通过关节角度计算与有限状态机判定,实现对深蹲、前屈伸展、俯卧撑、弓步蹲、臀桥、平板支撑、开合跳等 7 类训练动作的实时识别与计数;并结合节奏、稳定性、深度、对称性和完成率 5 个维度构建可解释的多维评分模型,辅以语音播报、节拍器与训练报告,形成"动作选择—实时识别—结果反馈—本地保存—条件上传"的完整训练闭环。系统采用 Vue 3 与 Spring Boot 的前后端分离架构,通过 IndexedDB 与渐进式 Web 应用(PWA)实现离线优先的数据保存与可安装部署。功能验证表明,系统在普通家用摄像头条件下能够稳定完成训练主流程,在弱网或未登录状态下仍可保留训练记录,具有部署门槛低、反馈直观、可解释性强等特点,为老年居家锻炼场景提供了一种低门槛的工程化方案。`;

children.push(new Paragraph({
  alignment: AlignmentType.JUSTIFIED,
  spacing: { line: 320, after: 80 },
  children: [
    tMix('摘要  ', { font: FONT_KAI, size: SIZE_5, bold: true }),
    tMix(abstractZh, { font: FONT_KAI, size: SIZE_5 }),
  ],
}));

// ---------- 中文关键词(五号楷体) ----------
children.push(new Paragraph({
  alignment: AlignmentType.JUSTIFIED,
  spacing: { line: 320, after: 80 },
  children: [
    tMix('关键词  ', { font: FONT_KAI, size: SIZE_5, bold: true }),
    tMix('姿态识别;居家健身;老年人;渐进式 Web 应用;离线优先;多维评分', { font: FONT_KAI, size: SIZE_5 }),
  ],
}));

// ---------- 中图法分类号 ----------
children.push(new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { line: 320, after: 320 },
  children: [
    tMix('中图法分类号  ', { font: FONT_SONG, size: SIZE_5, bold: true }),
    tMix('TP391.4', { font: FONT_EN, size: SIZE_5 }),
    tMix('   ', { size: SIZE_5 }),
    note('（TP391.4 = 信息处理:图像处理 / 计算机视觉。也可用 TP311.5 软件工程,二选一即可。）'),
  ],
}));

// ---------- 分隔说明 ----------
children.push(new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { before: 200, after: 80 },
  children: [tMix('— — — — — — — — — — 以下内容放在论文末尾(参考文献之后)— — — — — — — — — —', { font: FONT_SONG, size: SIZE_5S, color: '808080' })],
}));
children.push(blank());

// ---------- 作者介绍(英文在上、中文在下,小五号) ----------
children.push(new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { line: 320, after: 60 },
  children: [
    tMix('【Author Pinyin Name】, born in 【yyyy】. Undergraduate student. ', { font: FONT_EN, size: SIZE_5S }),
    tMix('His/Her main research interests include intelligent fitness applications, browser-side computer vision, and progressive web applications.', { font: FONT_EN, size: SIZE_5S }),
  ],
}));
children.push(new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { line: 320, after: 200 },
  children: [
    tMix('【作者姓名】,【yyyy】年生,本科在读。主要研究方向为智能健身应用、浏览器端计算机视觉与渐进式 Web 应用。', { font: FONT_SONG, size: SIZE_5S }),
  ],
}));

// ---------- 末尾:写作建议 ----------
children.push(new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { before: 320, after: 80 },
  children: [tMix('— — — — — — — — — — 论文整体改进建议(不复制进论文)— — — — — — — — — —', { font: FONT_SONG, size: SIZE_5S, color: '808080' })],
}));

const advices = [
  '【优点】结构完整、章节按"引言→相关技术与需求→总体设计→核心模块→验证→结论"展开,符合工程实践类论文范式;引言部分对 WHO 指南、综述研究、数字健康干预的引用论据充分。',
  '【优点】对项目当前不足的承认很到位:5.4 节明确指出"评分接口尚未完全接通""自动同步未闭环""未开展大规模老年用户实测"——这种诚实有助于评分,而不是减分项。',
  '【建议 1】引言中"老年人"立意与项目实际的通用居家用户定位略有偏差(代码里有挑战赛、动态、排行榜等社交模块)。建议在 2.2 系统需求处补一句:"虽然系统通用性面向居家用户,但本文重点考察其在适老化场景下的可行性",避免审稿人质疑选题与实现的对应。',
  '【建议 2】参考文献只有 6 条且都集中在引言。建议补 2~4 条关于 Vue/PWA/IndexedDB 或规则驱动评分相关的文献(如 W3C Service Worker 规范、IndexedDB 规范、PWA 综述等),分布到 2.1 与 4 章。',
  '【建议 3】公式 (1)、(2) 在文档中实际是空的(原文档里只剩逗号和编号),需要补全:公式 (1) 是 atan2 关节角公式,公式 (2) 是 S = 0.25R + 0.25T + 0.20D + 0.15Y + 0.15C。',
  '【建议 4】5 章实验部分较短,如果时间允许,可以补一个最简表格:测试人数 N 人 × 每动作做 K 次,记录识别成功率与平均评分,即使 N 小也比纯描述性文字更有说服力。如果不补,目前的写法也可接受,但要继续保留 5.4 与 6.2 的"未实测"声明。',
  '【建议 5】图 1/2/3 已经准备好(图1/2/3.png),论文中需要把图与正文对应位置插入并加上中英双语图题(模板要求中英双语,小五黑体)。图 4(训练页截图)、图 5(报告页截图)需要后续从实际项目中截。',
];
advices.forEach((line) => {
  children.push(new Paragraph({
    alignment: AlignmentType.JUSTIFIED,
    spacing: { line: 320, after: 60 },
    children: [tMix(line, { font: FONT_SONG, size: SIZE_5S })],
  }));
});

// =============== 输出 ===============

const doc = new Document({
  styles: {
    default: { document: { run: { font: FONT_SONG, size: SIZE_5 } } },
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },                       // A4
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } // 1 inch
      }
    },
    children,
  }],
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync('D:/备份/一级项目/中文实践论文/论文-标题摘要等前置内容.docx', buf);
  console.log('OK');
});
