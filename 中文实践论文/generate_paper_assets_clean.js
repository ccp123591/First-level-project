const fs = require("fs");
const path = require("path");
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  AlignmentType,
  WidthType,
  BorderStyle,
  HeadingLevel,
  Math,
  MathRun,
  MathFraction,
  MathSubScript,
} = require("C:/Users/24cpc/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/docx");

const OUT_FILES = [
  path.resolve(process.cwd(), "FitCoach论文表格与公式素材-完整版.docx"),
];

const pageWidth = 9360;
const colWidths = [2100, 4500, 2760];

const emptyBorder = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
const blackLine = { style: BorderStyle.SINGLE, size: 10, color: "000000" };

function p(text, opts = {}) {
  return new Paragraph({
    spacing: { after: 120, ...(opts.spacing || {}) },
    alignment: opts.alignment,
    children: [
      new TextRun({
        text,
        font: opts.font || "宋体",
        size: opts.size || 24,
        bold: opts.bold || false,
      }),
    ],
  });
}

function title(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 240, after: 160 },
    children: [new TextRun({ text, font: "黑体", size: 28, bold: true })],
  });
}

function tableCell(text, width, align = AlignmentType.CENTER, borders = {}) {
  return new TableCell({
    width: { size: width, type: WidthType.DXA },
    margins: { top: 80, bottom: 80, left: 120, right: 120 },
    borders: {
      top: borders.top || emptyBorder,
      bottom: borders.bottom || emptyBorder,
      left: emptyBorder,
      right: emptyBorder,
    },
    children: [
      new Paragraph({
        alignment: align,
        children: [new TextRun({ text, font: "宋体", size: 24 })],
      }),
    ],
  });
}

function buildTable1() {
  const rows = [
    ["模块", "具体需求", "当前实现情况"],
    ["训练输入", "支持动作选择、目标次数设置与训练开始控制", "已实现"],
    ["姿态识别", "调用摄像头并实时提取人体关键点信息", "已实现"],
    ["动作判定", "完成计数、状态判断与基础纠错提示", "已实现"],
    ["结果反馈", "输出综合评分、分项评分与训练报告", "已实现"],
    ["本地记录", "保存训练记录并支持历史查看与导出", "已实现"],
    ["后端支撑", "登录后上传记录并提供记录查询接口", "部分实现"],
    ["扩展功能", "计划、排行榜、动态等扩展业务模块", "部分实现"],
  ];

  return new Table({
    width: { size: pageWidth, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: rows.map((row, idx) => {
      const isHeader = idx === 0;
      const isLast = idx === rows.length - 1;
      const cellBorders = {
        top: isHeader ? blackLine : emptyBorder,
        bottom: isHeader || isLast ? blackLine : emptyBorder,
      };
      return new TableRow({
        children: [
          tableCell(row[0], colWidths[0], AlignmentType.CENTER, cellBorders),
          tableCell(row[1], colWidths[1], AlignmentType.LEFT, cellBorders),
          tableCell(row[2], colWidths[2], AlignmentType.CENTER, cellBorders),
        ],
      });
    }),
  });
}

function buildTable2() {
  const rows = [
    ["动作名称", "关键点选择", "阈值下限", "阈值上限", "动作类型"],
    ["深蹲", "髋-膝-踝", "90", "160", "计数型"],
    ["前屈伸展", "肩-髋-膝", "60", "160", "计数型"],
    ["俯卧撑", "肩-肘-腕", "80", "160", "计数型"],
    ["弓步蹲", "髋-膝-踝", "100", "170", "计数型"],
    ["臀桥", "肩-髋-膝", "150", "175", "计数型"],
    ["平板支撑", "肩-髋-踝", "-", "-", "时间型"],
    ["开合跳", "肩-肘-腕", "-", "-", "计数型"],
  ];

  const widths = [1800, 3000, 1500, 1500, 1560];

  return new Table({
    width: { size: pageWidth, type: WidthType.DXA },
    columnWidths: widths,
    rows: rows.map((row, idx) => {
      const isHeader = idx === 0;
      const isLast = idx === rows.length - 1;
      const cellBorders = {
        top: isHeader ? blackLine : emptyBorder,
        bottom: isHeader || isLast ? blackLine : emptyBorder,
      };
      return new TableRow({
        children: [
          tableCell(row[0], widths[0], AlignmentType.CENTER, cellBorders),
          tableCell(row[1], widths[1], AlignmentType.CENTER, cellBorders),
          tableCell(row[2], widths[2], AlignmentType.CENTER, cellBorders),
          tableCell(row[3], widths[3], AlignmentType.CENTER, cellBorders),
          tableCell(row[4], widths[4], AlignmentType.CENTER, cellBorders),
        ],
      });
    }),
  });
}

function buildTable3() {
  const rows = [
    ["功能模块", "验证内容", "验证结果"],
    ["训练页面", "动作选择、目标设置与训练开始流程", "通过"],
    ["姿态识别", "摄像头采集与人体关键点实时检测", "通过"],
    ["动作计数", "重复动作状态判断与次数累计", "通过"],
    ["结果反馈", "综合评分、分项评分与训练报告展示", "通过"],
    ["本地记录", "训练结果写入本地并支持历史查看", "通过"],
    ["离线可用", "弱网或未登录状态下完成基础训练", "基本通过"],
    ["后端同步", "登录后上传训练记录与记录查询", "部分通过"],
  ];

  return new Table({
    width: { size: pageWidth, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: rows.map((row, idx) => {
      const isHeader = idx === 0;
      const isLast = idx === rows.length - 1;
      const cellBorders = {
        top: isHeader ? blackLine : emptyBorder,
        bottom: isHeader || isLast ? blackLine : emptyBorder,
      };
      return new TableRow({
        children: [
          tableCell(row[0], colWidths[0], AlignmentType.CENTER, cellBorders),
          tableCell(row[1], colWidths[1], AlignmentType.LEFT, cellBorders),
          tableCell(row[2], colWidths[2], AlignmentType.CENTER, cellBorders),
        ],
      });
    }),
  });
}

function buildTable4() {
  const widths = [1900, 1800, 1800, 3860];
  const rows = [
    ["动作名称", "完成次数", "综合评分", "结果说明"],
    ["深蹲", "15", "86", "节奏较稳定，但下蹲深度仍有提升空间"],
    ["前屈伸展", "12", "82", "动作完成度较好，稳定性略有波动"],
    ["弓步蹲", "10", "78", "左右侧动作存在一定不对称现象"],
    ["臀桥", "15", "89", "整体完成度较高，节奏控制较均匀"],
  ];

  return new Table({
    width: { size: pageWidth, type: WidthType.DXA },
    columnWidths: widths,
    rows: rows.map((row, idx) => {
      const isHeader = idx === 0;
      const isLast = idx === rows.length - 1;
      const cellBorders = {
        top: isHeader ? blackLine : emptyBorder,
        bottom: isHeader || isLast ? blackLine : emptyBorder,
      };
      return new TableRow({
        children: [
          tableCell(row[0], widths[0], AlignmentType.CENTER, cellBorders),
          tableCell(row[1], widths[1], AlignmentType.CENTER, cellBorders),
          tableCell(row[2], widths[2], AlignmentType.CENTER, cellBorders),
          tableCell(row[3], widths[3], AlignmentType.LEFT, cellBorders),
        ],
      });
    }),
  });
}

function sub(base, lower) {
  return new MathSubScript({
    children: [new MathRun(base)],
    subScript: [new MathRun(lower)],
  });
}

function formula1() {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [
      new Math({
        children: [
          new MathRun("θ = atan2("),
          new MathRun("|("),
          sub("a", "x"),
          new MathRun(" - "),
          sub("b", "x"),
          new MathRun(")("),
          sub("c", "y"),
          new MathRun(" - "),
          sub("b", "y"),
          new MathRun(") - ("),
          sub("a", "y"),
          new MathRun(" - "),
          sub("b", "y"),
          new MathRun(")("),
          sub("c", "x"),
          new MathRun(" - "),
          sub("b", "x"),
          new MathRun(")|, ("),
          sub("a", "x"),
          new MathRun(" - "),
          sub("b", "x"),
          new MathRun(")("),
          sub("c", "x"),
          new MathRun(" - "),
          sub("b", "x"),
          new MathRun(") + ("),
          sub("a", "y"),
          new MathRun(" - "),
          sub("b", "y"),
          new MathRun(")("),
          sub("c", "y"),
          new MathRun(" - "),
          sub("b", "y"),
          new MathRun(")) × "),
          new MathFraction({
            numerator: [new MathRun("180")],
            denominator: [new MathRun("π")],
          }),
        ],
      }),
      new TextRun({ text: "\t(1)", font: "Times New Roman", size: 24 }),
    ],
  });
}

function formula2() {
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    children: [
      new Math({
        children: [new MathRun("S = 0.25R + 0.25T + 0.20D + 0.15Y + 0.15C")],
      }),
      new TextRun({ text: "\t(2)", font: "Times New Roman", size: 24 }),
    ],
  });
}

const doc = new Document({
  sections: [
    {
      properties: {
        page: {
          size: { width: 12240, height: 15840 },
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
        },
      },
      children: [
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 180 },
          children: [
            new TextRun({
              text: "FitCoach论文表格与公式素材",
              font: "黑体",
              size: 32,
              bold: true,
            }),
          ],
        }),
        p("说明：本文件用于集中存放论文正文中需要插入的表格和公式，便于后续直接复制粘贴到最终 Word 成稿中。"),

        title("表1 系统功能需求汇总"),
        p("Table 1 System Functional Requirements Summary", {
          alignment: AlignmentType.CENTER,
          font: "Times New Roman",
        }),
        p("表1 系统功能需求汇总", { alignment: AlignmentType.CENTER }),
        buildTable1(),

        title("表2 主要训练动作与判定参数"),
        p("Table 2 Main Training Actions and Judgment Parameters", {
          alignment: AlignmentType.CENTER,
          font: "Times New Roman",
        }),
        p("表2 主要训练动作与判定参数", { alignment: AlignmentType.CENTER }),
        buildTable2(),

        title("表3 系统功能验证结果"),
        p("Table 3 Functional Verification Results of the FitCoach System", {
          alignment: AlignmentType.CENTER,
          font: "Times New Roman",
        }),
        p("表3 FitCoach系统功能验证结果", { alignment: AlignmentType.CENTER }),
        buildTable3(),

        title("表4 典型训练结果示例"),
        p("Table 4 Example of Typical Training Results", {
          alignment: AlignmentType.CENTER,
          font: "Times New Roman",
        }),
        p("表4 典型训练结果示例", { alignment: AlignmentType.CENTER }),
        buildTable4(),
        p("注：表4数据为系统运行过程中的典型示例，用于展示结果页面的呈现方式。"),

        title("公式1 关节角计算公式"),
        p("建议放置位置：第4章“动作判定与计数模块设计”部分。"),
        formula1(),
        p("其中，θ 表示关节角度，a、b、c 分别表示构成关节角的三个关键点坐标。"),

        title("公式2 综合评分公式"),
        p("建议放置位置：第4章“多维评分模块设计”部分。"),
        formula2(),
        p("其中，S 为综合评分，R 为节奏分，T 为稳定分，D 为深度分，Y 为对称分，C 为完成率分。"),

        title("参考文献"),
        p("[1] Bull F C, Al-Ansari S S, Biddle S, et al. World Health Organization 2020 guidelines on physical activity and sedentary behaviour[J]. British Journal of Sports Medicine, 2020, 54(24): 1451-1462."),
        p("[2] Di Lorito C, Long A, Byrne A, et al. Exercise interventions for older adults: a systematic review of meta-analyses[J]. Journal of Sport and Health Science, 2021, 10(1): 29-47."),
        p("[3] Sherrington C, Fairhall N, Wallbank G, et al. Exercise for preventing falls in older people living in the community: an abridged Cochrane systematic review[J]. British Journal of Sports Medicine, 2020, 54(15): 885-891."),
        p("[4] Solis-Navarro L, Gismero A, Fernandez-Jane C, et al. Effectiveness of home-based exercise delivered by digital health in older adults: a systematic review and meta-analysis[J]. Age and Ageing, 2022, 51(11): afac243."),
        p("[5] Lugaresi C, Tang J, Nash H, et al. MediaPipe: a framework for building perception pipelines[OL]. [2026-05-02]. https://arxiv.org/abs/1906.08172."),
        p("[6] Bazarevsky V, Grishchenko I, Raveendran K, et al. BlazePose: on-device real-time body pose tracking[OL]. [2026-05-02]. https://arxiv.org/abs/2006.10204."),

        title("文献核验链接（备查）"),
        p("[1] PMID: 33239350；DOI: 10.1136/bjsports-2020-102955；链接：https://pubmed.ncbi.nlm.nih.gov/33239350/"),
        p("[2] DOI: 10.1016/j.jshs.2020.06.003；链接：https://www.sciencedirect.com/science/article/pii/S2095254620300697"),
        p("[3] PMID: 31792067；DOI: 10.1136/bjsports-2019-101512；链接：https://pubmed.ncbi.nlm.nih.gov/31792067/"),
        p("[4] PMID: 36346736；DOI: 10.1093/ageing/afac243；链接：https://pubmed.ncbi.nlm.nih.gov/36346736/"),
        p("[5] DOI: 10.48550/arXiv.1906.08172；链接：https://arxiv.org/abs/1906.08172"),
        p("[6] DOI: 10.48550/arXiv.2006.10204；链接：https://arxiv.org/abs/2006.10204"),
      ],
    },
  ],
});

Packer.toBuffer(doc).then((buffer) => {
  for (const out of OUT_FILES) {
    fs.writeFileSync(out, buffer);
  }
});
