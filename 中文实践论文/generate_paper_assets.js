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
  TabStopType,
  TabStopPosition,
} = require("C:/Users/24cpc/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/docx");

const OUT = path.resolve(process.cwd(), "fitcoach-paper-assets.docx");

const pageWidth = 9360;
const colWidths = [2100, 4500, 2760];

const emptyBorder = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
const blackLine = { style: BorderStyle.SINGLE, size: 10, color: "000000" };

function p(text, opts = {}) {
  return new Paragraph({
    spacing: { after: 120, ...(opts.spacing || {}) },
    alignment: opts.alignment,
    tabStops: opts.tabStops,
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

function sectionTitle(text) {
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
        p(
          "说明：本文件用于集中存放论文正文中需要插入的表格和公式，便于后续直接复制粘贴到最终 Word 成稿中。",
          { spacing: { after: 200 } }
        ),
        p("使用方式：正文中保留“表格位置”或“公式位置”说明，最终排版时从本文件复制到对应位置即可。"),

        sectionTitle("表1 系统功能需求汇总"),
        p("Table 1 System Functional Requirements Summary", {
          alignment: AlignmentType.CENTER,
          font: "Times New Roman",
        }),
        p("表1 系统功能需求汇总", { alignment: AlignmentType.CENTER }),
        buildTable1(),

        sectionTitle("公式1 关节角计算公式"),
        p("建议放置位置：第4章“动作判定与计数模块设计”部分。"),
        p("以下内容为可直接粘贴到 Word 公式编辑器中的 LaTeX："),
        p(
          String.raw`\theta = \operatorname{atan2}\left(\left|(a_x-b_x)(c_y-b_y)-(a_y-b_y)(c_x-b_x)\right|,\,(a_x-b_x)(c_x-b_x)+(a_y-b_y)(c_y-b_y)\right)\times \frac{180}{\pi}`,
          {
            font: "Times New Roman",
          }
        ),
        p("公式编号： (1)"),
        p("其中，\\theta 表示关节角度，a、b、c 分别表示构成关节角的三个关键点坐标。"),

        sectionTitle("公式2 综合评分公式"),
        p("建议放置位置：第4章“多维评分模块设计”部分。"),
        p("以下内容为可直接粘贴到 Word 公式编辑器中的 LaTeX："),
        p(String.raw`S = 0.25R + 0.25T + 0.20D + 0.15Y + 0.15C`, {
          font: "Times New Roman",
        }),
        p("公式编号： (2)"),
        p("其中，S 为综合评分，R 为节奏分，T 为稳定分，D 为深度分，Y 为对称分，C 为完成率分。"),

        sectionTitle("后续待补充"),
        p("表2 主要训练动作与判定参数"),
        p("表3 系统功能验证结果"),
        p("表4 典型训练结果示例"),
      ],
    },
  ],
});

Packer.toBuffer(doc).then((buffer) => fs.writeFileSync(OUT, buffer));
