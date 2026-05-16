// 生成"论文冗余修改说明"docx
const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, AlignmentType, HeadingLevel,
        Table, TableRow, TableCell, BorderStyle, WidthType, ShadingType } = require('docx');

const FONT_HEI = 'SimHei';
const FONT_SONG = 'SimSun';
const FONT_EN = 'Times New Roman';

// 字号 half-points: 三号=32, 四号=28, 小四=24, 五号=21, 小五=18, 六号=15
const SZ_TITLE = 32;
const SZ_H1 = 24;     // 小四黑
const SZ_H2 = 22;
const SZ_BODY = 21;   // 五号
const SZ_NOTE = 18;

const t = (text, opts = {}) => new TextRun({
  text,
  font: opts.font || FONT_SONG,
  size: opts.size || SZ_BODY,
  bold: !!opts.bold,
  color: opts.color,
  italics: !!opts.italics,
});

const p = (runs, opts = {}) => new Paragraph({
  alignment: opts.align || AlignmentType.LEFT,
  spacing: { line: 360, before: opts.before || 0, after: opts.after !== undefined ? opts.after : 100 },
  ...(opts.indent ? { indent: opts.indent } : {}),
  children: Array.isArray(runs) ? runs : [runs],
});

const h1 = (text) => p(t(text, { font: FONT_HEI, size: SZ_H1, bold: true }), { before: 240, after: 120 });
const h2 = (text) => p(t(text, { font: FONT_HEI, size: SZ_H2, bold: true }), { before: 160, after: 80 });

const blank = () => new Paragraph({ children: [new TextRun('')] });

const block = (label, text, opts = {}) => {
  // 高亮的对照块 — 用浅色背景
  const cell = new TableCell({
    borders: {
      top: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
      bottom: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
      left: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
      right: { style: BorderStyle.SINGLE, size: 4, color: opts.color || 'D9D9D9' },
    },
    shading: { fill: opts.fill || 'F8F8F8', type: ShadingType.CLEAR },
    margins: { top: 100, bottom: 100, left: 160, right: 160 },
    width: { size: 9026, type: WidthType.DXA },
    children: [
      new Paragraph({
        spacing: { line: 320, after: 60 },
        children: [t(label, { font: FONT_HEI, size: 19, bold: true, color: opts.labelColor || '595959' })],
      }),
      ...text.split('\n').map(line => new Paragraph({
        spacing: { line: 340, after: 40 },
        children: [t(line, { size: SZ_BODY })],
      })),
    ],
  });
  return new Table({
    width: { size: 9026, type: WidthType.DXA },
    columnWidths: [9026],
    rows: [new TableRow({ children: [cell] })],
  });
};

// ============== 内容 ==============

const children = [];

// 标题
children.push(p(t('FitCoach 论文冗余修改说明', { font: FONT_HEI, size: SZ_TITLE, bold: true }),
  { align: AlignmentType.CENTER, after: 120 }));
children.push(p(t('—— 共 6 处需要改,按"原文 → 改成"对照替换即可', { font: FONT_SONG, size: SZ_BODY, color: '7F7F7F' }),
  { align: AlignmentType.CENTER, after: 240 }));

// 总体说明
children.push(h1('总体说明'));
children.push(p([
  t('我把全文 6 章逐段比对了一遍,发现冗余主要集中在三类问题上:'),
]));
children.push(p([t('1. ', { bold: true }), t('"5 维评分清单"被列出了 6 次', { bold: true }), t(',只有 §4.3 是详细解释,其他位置应该用"多维评分"一笔带过。')]));
children.push(p([t('2. ', { bold: true }), t('§3.3 与 §3.4 严重重叠', { bold: true }), t(',两节都在讲"本地优先 + 数据存储",建议合并删减。')]));
children.push(p([t('3. ', { bold: true }), t('"无需专用硬件 / 不上传云端"立论说了 4 次', { bold: true }), t(',应保留 §1 引言和 §6.1 结论,中间不再展开。')]));
children.push(p([t('下面 6 处按"严重程度从高到低"排序,每处都给出"原文"和"建议改成"。', { color: '7F7F7F' })]));
children.push(blank());

// =================== 修改 1 ===================
children.push(h1('修改 1:§3.3 与 §3.4 合并(最严重的重复)'));

children.push(p(t('问题:这两节几乎在讲同一件事 — 都是"训练结果先写本地、再条件上传"。建议直接合并为一节,标题改为"3.3 数据流与本地优先存储设计",原 §3.4 整节删除。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§3.3 + §3.4 三段):', { bold: true })));
children.push(block('删除',
'§3.3 第二段:在数据流处理上,系统采用本地优先的思路。训练结果形成后,记录首先写入浏览器端本地数据库,再在满足登录条件时尝试上传后端。这样可以避免训练结果完全依赖网络提交,从而提高系统在真实家庭环境中的稳定性。\n\n§3.4 第一段:在数据存储方面,系统采用本地存储与服务端存储相结合的方式。前端使用 IndexedDB 保存训练记录,使用本地配置存储保存语音、节拍和训练参数等设置项;后端数据库则负责保存用户信息、训练记录及扩展业务数据。这样的双层存储结构既能满足本地快速训练的需要,也为后续长期记录管理提供了基础。\n\n§3.4 第二段:在前后端协同方面,训练主流程中的姿态识别、计数和评分主要在浏览器端完成,后端主要承担认证、查询、导出和扩展接口支撑等任务。这样的设计使系统既保留了前端本地运行的即时性,又具备后端业务扩展能力。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成(把 §3.3 和 §3.4 合并为一节,标题改名,正文如下):', { bold: true })));
children.push(p(t('3.3 数据流与本地优先存储设计', { bold: true, font: FONT_HEI, size: SZ_BODY })));
children.push(block('保留并合并',
'FitCoach 的核心业务流程可以概括为"动作选择—姿态识别—动作判定—结果反馈—本地保存—条件上传"。用户进入训练页面后,前端初始化姿态识别模型并调用摄像头采集视频,关键点信息进入动作判定逻辑完成角度计算、状态判断和训练计数,训练结束后系统生成评分结果和训练报告。\n\n在数据存储上,系统采用前端 IndexedDB 与后端数据库双层结构:训练结果先写入浏览器本地,在用户已登录且网络可用时再尝试上传后端;本地配置存储则用于保存语音、节拍等参数。这种"本地优先 + 条件上传"的设计避免了训练结果完全依赖网络提交,使系统在真实家庭环境下仍能保持训练主流程的稳定运行,同时为后续长期记录管理保留扩展能力。\n\n(原 §3.4 整节删除,§3 章节后面接 §4)',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 2 ===================
children.push(h1('修改 2:§4.3 之外的 5 维清单全部精简'));

children.push(p(t('问题:"节奏、稳定性、深度、对称性和完成率"这串清单在摘要、§1、§2.2、§4.3、§5.3 出现了 6 次。读者第二次看到就够了。除 §4.3 是详细解释保留不动外,其余位置全部改成"多维评分"或"5 个维度"一笔带过。', { color: '595959' })));
children.push(blank());

children.push(p(t('改 1:§1 引言最后一段', { bold: true })));
children.push(block('原文',
'……第三,基于规则驱动方式实现节奏、稳定性、深度、对称性和完成率等多维可解释评分,并结合语音反馈增强训练结果的直观性与可理解性。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(block('改成',
'……第三,基于规则驱动方式实现可解释的多维评分,并结合语音反馈增强训练结果的直观性与可理解性。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

children.push(p(t('改 2:§2.2 功能需求第一段', { bold: true })));
children.push(block('原文',
'……还需要给出训练结果评分和训练报告,使用户能够从节奏、稳定性、深度、对称性和完成率等方面了解自身训练表现。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(block('改成',
'……还需要给出训练结果评分和训练报告,使用户能够从多个维度了解自身训练表现。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

children.push(p(t('改 3:§5.3 典型训练结果分析第一段', { bold: true })));
children.push(block('原文',
'以深蹲等重复性较强的训练动作为例,系统能够在训练结束后输出次数、时长和综合评分等结果,并进一步给出节奏、稳定性、深度、对称性和完成率等维度信息。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(block('改成',
'以深蹲等重复性较强的训练动作为例,系统能够在训练结束后输出次数、时长、综合评分以及前述 5 个维度的分项评分。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 3 ===================
children.push(h1('修改 3:删掉 §4.1 中关键点关节例子(已被表 2 覆盖)'));

children.push(p(t('问题:§4.1 第二段、§4.2 第三段、表 2 都在讲"哪个动作用哪些关节",讲了三遍。表 2 信息最完整最权威,§4.1 / §4.2 的例子可以删一处。建议删 §4.1 的,因为 §4.1 是入口章节,没必要陷入细节。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§4.1 第二段):', { bold: true })));
children.push(block('删一句',
'在实现方式上,系统根据不同训练动作选择对应的关键关节区域。例如,深蹲和弓步蹲主要关注髋、膝、踝之间的变化,前屈和臀桥则更多关注肩、髋、膝等关键点关系。通过这种方式,姿态识别模块输出的不只是通用人体关键点,而是能够为后续动作判定直接服务的结构化信息。对于本项目而言,该模块的重点不在于复杂模型创新,而在于以较低门槛支撑稳定、实时的训练识别过程。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成(删掉"例如…关键点关系"一句,具体动作的对应关系交给 §4.2 的表 2 展示):', { bold: true })));
children.push(block('改成',
'在实现方式上,系统根据不同训练动作选择对应的关键关节区域,具体对应关系见 §4.2 表 2。通过这种方式,姿态识别模块输出的不只是通用人体关键点,而是能够为后续动作判定直接服务的结构化信息。对于本项目而言,该模块的重点不在于复杂模型创新,而在于以较低门槛支撑稳定、实时的训练识别过程。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 4 ===================
children.push(h1('修改 4:删掉 §2.1 中"基础平滑处理"一句(与 §4.2 完全重复)'));

children.push(p(t('问题:§2.1 写"对关键点输出进行基础平滑处理,以减少帧间抖动",§4.2 又写"系统对角度序列进行了基础平滑处理,以减少摄像头抖动" — 两句几乎一模一样,§4.2 是详细介绍位置,§2.1 那句可以删。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§2.1 第二段):', { bold: true })));
children.push(block('删半句',
'在姿态识别方面,系统选用 MediaPipe Pose 与 BlazePose 作为浏览器端关键点检测方案。对于本项目而言,该技术的实际配置重点在于:针对不同训练动作选取对应的关键关节区域进行检测,同时对关键点输出进行基础平滑处理,以减少帧间抖动对后续动作判定的影响。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成:', { bold: true })));
children.push(block('改成',
'在姿态识别方面,系统选用 MediaPipe Pose 与 BlazePose 作为浏览器端关键点检测方案。对于本项目而言,该技术的实际配置重点在于针对不同训练动作选取对应的关键关节区域进行检测,具体阈值与计数逻辑详见 §4.2。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 5 ===================
children.push(h1('修改 5:§5.2 第一句的训练闭环改为简单回扣'));

children.push(p(t('问题:训练流程链("动作选择—…—本地保存")在摘要、§3.3、§5.2 共出现 3 次,而且每次的"工序名"还略不一样(实时识别 / 实时计数 / 摄像头识别)。§5.2 是验证章节,没必要再把链条复述一遍,改为一句简单回扣即可。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§5.2 第一段):', { bold: true })));
children.push(block('原文',
'在功能测试中,FitCoach 顺利完成了"动作选择—摄像头识别—实时计数—结果评分—本地保存"的核心训练闭环,各环节衔接稳定,未出现明显中断或异常。对于老年居家用户而言,整个操作流程无需安装额外软件或设备,步骤较为简洁,具有一定实际应用基础。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('改成:', { bold: true })));
children.push(block('改成',
'在功能测试中,FitCoach 顺利完成了 §3.3 所述的核心训练闭环,各环节衔接稳定,未出现明显中断或异常。整个操作流程无需安装额外软件或专用设备,在老年居家场景下具有一定实际应用基础。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235' }));
children.push(blank());

// =================== 修改 6 ===================
children.push(h1('修改 6:§3.1 末段 + §4.1 首段中"无需专用硬件"立论合并删减'));

children.push(p(t('问题:"无需专用硬件 / 不上传云端 / 减少网络波动影响"这套立论在 §1、§3.1、§4.1、§6.1 共说了 4 次。§1 是引言铺垫,§6.1 是结论回扣,这两处必须保留;中间的 §3.1 末段和 §4.1 首段重复,合并删一段即可。', { color: '595959' })));
children.push(blank());

children.push(p(t('原文(§3.1 末段):', { bold: true })));
children.push(block('整段删除',
'这种总体架构对于老年居家使用场景具有较好的适配性。一方面,训练核心能力主要部署在浏览器端,用户不需要额外安装复杂软件,也不需要依赖专用硬件设备;另一方面,后端服务并不直接介入每一次动作识别过程,从而减少了网络波动对训练主流程的影响。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(p(t('保留(§4.1 首段不动):', { bold: true })));
children.push(block('保留',
'浏览器端姿态识别模块是 FitCoach 训练主链路的起点,其主要任务是从摄像头视频流中提取人体关键点信息,并为后续动作判定和结果评分提供基础数据。系统在训练开始后初始化姿态识别模型,并持续读取视频帧,在浏览器端完成关键点检测。由于这一过程不依赖专用传感器,也不需要将视频上传到云端,因此较适合居家老年用户的实际使用环境。',
{ fill: 'EAEAEA', color: 'A6A6A6', labelColor: '595959' }));
children.push(p(t('删除 §3.1 末段后,§3.1 直接以"图 2 FitCoach 一级项目总体架构图"结束,衔接 §3.2 没问题。', { color: '7F7F7F', size: SZ_NOTE })));
children.push(blank());

// =================== 总结 ===================
children.push(h1('改完之后整体效果'));
children.push(p(t('做完上面 6 处修改后,论文大约会缩减 350~450 字,但论证密度更高、读起来不会有"已经讲过了"的感觉。各章节的功能定位也更清晰:', { color: '595959' })));
children.push(p(t('• §1 引言 → 立论(为什么做)')));
children.push(p(t('• §2 → 技术选型 + 需求')));
children.push(p(t('• §3 → 总体设计 + 数据流(合并后)')));
children.push(p(t('• §4 → 4 个核心模块逐一展开')));
children.push(p(t('• §5 → 验证 + 不足')));
children.push(p(t('• §6 → 结论 + 展望')));
children.push(blank());

children.push(p(t('提示:6 处都不影响参考文献编号 [1]~[6],不需要重新调整引用。', { color: '7F7F7F', italics: true })));

children.push(blank());
children.push(p(t('如果想再精简一点(可选)', { bold: true, font: FONT_HEI })));
children.push(p(t('§3.2 功能模块划分把每个模块的职责又列了一遍,与 §3.1 的"前端训练层职责"列表有重合。如果你觉得还想再压一压,可以把 §3.2 的"训练模块主要负责…"那种逐项说明改成一句话总览,但这处影响小,不改也行。', { color: '7F7F7F', size: SZ_NOTE })));

// =================== 输出 ===================

const doc = new Document({
  styles: { default: { document: { run: { font: FONT_SONG, size: SZ_BODY } } } },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    children,
  }],
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync('D:/备份/一级项目/中文实践论文/论文-冗余修改说明.docx', buf);
  console.log('OK');
});
