// 补充:中文摘要 AI 味改写
const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, AlignmentType,
        Table, TableRow, TableCell, BorderStyle, WidthType, ShadingType } = require('docx');

const FONT_HEI = 'SimHei';
const FONT_SONG = 'SimSun';
const FONT_KAI = 'KaiTi';

const SZ_TITLE = 32;
const SZ_H1 = 24;
const SZ_BODY = 21;
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
  children: Array.isArray(runs) ? runs : [runs],
});

const h1 = (text) => p(t(text, { font: FONT_HEI, size: SZ_H1, bold: true }), { before: 240, after: 120 });
const blank = () => new Paragraph({ children: [new TextRun('')] });

const block = (label, text, opts = {}) => {
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
        spacing: { line: 360, after: 40 },
        children: [t(line, { font: opts.font || FONT_KAI, size: SZ_BODY })],
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

children.push(p(t('补充:中文摘要 AI 味改写', { font: FONT_HEI, size: SZ_TITLE, bold: true }),
  { align: AlignmentType.CENTER, after: 120 }));
children.push(p(t('—— 上一份说明里漏处理了中文摘要,这里单独补上', { font: FONT_SONG, size: SZ_BODY, color: '7F7F7F' }),
  { align: AlignmentType.CENTER, after: 240 }));

children.push(h1('中文摘要存在的 AI 痕迹'));
children.push(p([t('1. ', { bold: true }), t('3 个分号长句强行并列', { bold: true }), t(' — "系统以普通摄像头…;通过关节角度…;并结合…5 个维度…" 这种排比是 AI 写中文摘要的典型节奏。')]));
children.push(p([t('2. ', { bold: true }), t('"实现对…等 7 类训练动作"', { bold: true }), t(' — 已经写了具体数字"7 类",还要加"等",前后矛盾,这是 AI 写作的"凑字数"惯性。')]));
children.push(p([t('3. ', { bold: true }), t('"动作选择—实时识别—结果反馈—本地保存—条件上传"5 段破折号链', { bold: true }), t(' — 同样是过分工整的对仗。')]));
children.push(p([t('4. ', { bold: true }), t('"具有部署门槛低、反馈直观、可解释性强等特点"', { bold: true }), t(' — "具有 X、Y、Z 等特点" + 三字短语并列,极典型 AI 套话。')]));
children.push(p([t('5. ', { bold: true }), t('"低门槛的工程化方案"', { bold: true }), t(' — 抽象名词堆叠,"工程化方案"这种说法极少在真人写作中出现。')]));
children.push(p([t('6. ', { bold: true }), t('"门槛较高 → 低门槛"', { bold: true }), t(' — 同一摘要里 "门槛"出现 2 次。')]));
children.push(blank());

children.push(h1('原中文摘要(待替换)'));
children.push(block('原文',
'针对居家老年人在日常锻炼中缺少现场指导、训练节奏难以把握、复杂数字产品操作门槛较高等问题,本文设计并实现了一个基于浏览器端姿态识别的智能健身陪练系统FitCoach。系统以普通摄像头为输入,利用MediaPipePose框架在浏览器端完成人体关键点检测;通过关节角度计算与有限状态机判定,实现对深蹲、前屈伸展、俯卧撑、弓步蹲、臀桥、平板支撑、开合跳等7类训练动作的实时识别与计数;并结合节奏、稳定性、深度、对称性和完成率5个维度构建可解释的多维评分模型,辅以语音播报、节拍器与训练报告,形成"动作选择—实时识别—结果反馈—本地保存—条件上传"的完整训练闭环。系统采用Vue3与SpringBoot的前后端分离架构,通过IndexedDB与渐进式Web应用(PWA)实现离线优先的数据保存与可安装部署。功能验证表明,系统在普通家用摄像头条件下能够稳定完成训练主流程,在弱网或未登录状态下仍可保留训练记录,具有部署门槛低、反馈直观、可解释性强等特点,为老年居家锻炼场景提供了一种低门槛的工程化方案。',
{ fill: 'FBE5E5', color: 'E08080', labelColor: 'C00000' }));
children.push(blank());

children.push(h1('改写后中文摘要(直接复制替换)'));

children.push(p(t('字数 ~340 字,符合模板"300 字左右"的要求。', { color: '7F7F7F', size: SZ_NOTE })));

children.push(block('改成',
'针对居家老年人在日常锻炼中缺少现场指导、训练节奏不易把握、复杂数字产品上手门槛高等问题,本文设计并实现了一个基于浏览器端姿态识别的智能健身陪练系统 FitCoach。系统只需要普通摄像头,通过 MediaPipe Pose 框架在浏览器端完成人体关键点检测,再用关节角度计算和有限状态机识别并计数 7 种常见训练动作,包括深蹲、前屈伸展、俯卧撑、弓步蹲、臀桥、平板支撑和开合跳。除动作计数外,系统从节奏、稳定性、深度、对称性和完成率 5 个维度给出可解释的分项评分,并配合语音播报、节拍器和训练报告组成一套完整的训练流程。整体架构采用 Vue 3 加 Spring Boot 前后端分离,训练记录先写浏览器 IndexedDB、登录联网时再上传后端,配合渐进式 Web 应用(PWA)的安装能力,使系统在弱网或未登录时也不会丢失训练记录。功能测试表明,普通家用摄像头条件下系统可以稳定跑完整个训练流程,部署不依赖专用硬件、反馈不依赖云端处理,基本满足老年用户在家场景下的连续使用需求。',
{ fill: 'E2F0D9', color: '70AD47', labelColor: '548235', font: FONT_KAI }));
children.push(blank());

children.push(h1('主要改动点说明'));

const changes = [
  ['"系统以普通摄像头为输入,利用…;通过…;并结合…"3 个长分号句', '拆成 3 个独立的短句,并把"输入"改为更口语的"只需要普通摄像头"'],
  ['"实现对…等 7 类训练动作"', '"等 7 类"改成"7 种",并把动作名直接顺着列出来,不另起分句'],
  ['"动作选择—实时识别—结果反馈—本地保存—条件上传"破折号链', '删掉这串破折号,改成"先写本地、登录后再上传"的动词描述'],
  ['"具有部署门槛低、反馈直观、可解释性强等特点"', '改成"部署不依赖专用硬件、反馈不依赖云端处理" — 把抽象描述换成具体的"不依赖什么"'],
  ['"为老年居家锻炼场景提供了一种低门槛的工程化方案"', '改成"基本满足老年用户在家场景下的连续使用需求" — 砍掉"工程化方案"这种抽象名词'],
  ['"门槛较高" + "低门槛"', '前者改"上手门槛高",后者直接删除,避免重复'],
  ['"动作选择—实时识别—结果反馈—本地保存—条件上传"5 段对仗', '完全打散,改成"训练记录先写浏览器 IndexedDB、登录联网时再上传后端"'],
];

changes.forEach(([loc, after]) => {
  children.push(p([
    t('• ', { color: '595959' }),
    t(loc, { color: 'C00000' }),
  ]));
  children.push(p([t('  → ', { color: '548235' }), t(after, { color: '548235' })], { after: 80 }));
});

children.push(blank());

children.push(h1('英文摘要也要相应改'));
children.push(p(t('上一份"AI 味修改说明"已经给了英文摘要的去 em-dash 改写版本,但那一版的内容(7 个动作、5 个维度等)和这次改写的中文摘要应该保持对应。建议把英文摘要也照着新中文版重写一次,核心信息不变,但句式和顺序与中文一致。', { color: '595959' })));
children.push(blank());

children.push(p(t('对照英文版(已与新中文摘要对齐):', { bold: true })));

children.push(block('English Abstract',
'Elderly users often face several difficulties in home-based physical exercise, such as the lack of on-site guidance, difficulty in maintaining proper training rhythm, and the high learning curve of many digital products. To address these issues, this paper designs and implements an intelligent fitness coaching system, FitCoach, based on browser-side human pose recognition. The system only requires a standard webcam: it uses the MediaPipe Pose framework to detect body keypoints directly in the browser, and then applies joint-angle computation and a finite state machine to recognize and count seven common training movements, including squat, forward stretch, push-up, lunge, glute bridge, plank and jumping jack. Beyond simple counting, the system gives interpretable per-dimension scores along five dimensions (rhythm, stability, depth, symmetry, and completion rate), and combines them with text-to-speech feedback, a metronome and a post-training report to form a complete training workflow. The architecture follows a Vue 3 / Spring Boot front-end and back-end split: training records are first written to IndexedDB in the browser and only uploaded to the back-end when the user is logged in and online, while the Progressive Web Application (PWA) layer adds installable deployment, so that records are preserved even under weak network or unauthenticated states. Functional tests show that under ordinary home-camera conditions the system can stably complete the entire training workflow, without relying on dedicated hardware or cloud-side processing, which fits the continuous-use requirements of elderly users at home.',
{ fill: 'EAEAEA', color: 'A6A6A6', labelColor: '595959', font: 'Times New Roman' }));
children.push(blank());

children.push(p(t('注意:英文摘要里 "(rhythm, stability, depth, symmetry, and completion rate)" 用了括号,不再用 em-dash,符合上一份说明里"修改 1"的整体方向。', { color: '7F7F7F', size: SZ_NOTE })));

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
  fs.writeFileSync('D:/备份/一级项目/中文实践论文/论文-AI味修改说明-补充中文摘要.docx', buf);
  console.log('OK');
});
