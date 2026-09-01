import os
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

from reportlab.lib.pagesizes import letter
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors

# -------------------------------------------------------------
# 1. GENERATE MICROSOFT WORD (.DOCX)
# -------------------------------------------------------------
def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def create_docx(filename):
    doc = Document()
    
    # Page setup - Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)
        
    # Styles
    title_style = doc.styles['Title']
    title_style.font.name = 'Calibri'
    title_style.font.size = Pt(24)
    title_style.font.bold = True
    title_style.font.color.rgb = RGBColor(0x1F, 0x29, 0x37) # Dark slate
    
    # Header Title
    p_title = doc.add_paragraph("Analog Anchor: iOS Architecture Guide")
    p_title.style = 'Title'
    p_title.paragraph_format.space_after = Pt(2)
    
    p_sub = doc.add_paragraph("Building Reboot-Proof Offline Challenges & Focus Modes on Apple iOS (No VPN Needed)")
    p_sub.runs[0].font.size = Pt(13)
    p_sub.runs[0].font.color.rgb = RGBColor(0x4B, 0x55, 0x63)
    p_sub.runs[0].font.italic = True
    p_sub.paragraph_format.space_after = Pt(14)
    
    # Divider
    p_div = doc.add_paragraph()
    p_div.paragraph_format.space_after = Pt(12)
    p_div_run = p_div.add_run("―" * 48)
    p_div_run.font.color.rgb = RGBColor(0x9C, 0xA3, 0xAF)

    # 1. Mindset Shift
    h1 = doc.add_heading("1. The Mindset Shift: Android vs. iOS", level=1)
    h1.runs[0].font.color.rgb = RGBColor(0x11, 0x18, 0x27)
    
    doc.add_paragraph(
        "On Android, an offline challenge app acts like a security guard constantly running in the background, "
        "managing a dummy VPN tunnel to sink packets and listening to boot/unlock events. "
        "On iOS, third-party apps are not allowed to sit in the background or monitor other apps. Instead, iOS utilizes an Orchestrator Model:"
    )
    
    # Callout Box
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    set_cell_background(cell, "F3F4F6")
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    p_call = cell.paragraphs[0]
    p_call.paragraph_format.space_after = Pt(0)
    r = p_call.add_run("Key Architectural Principle: ")
    r.bold = True
    r.font.color.rgb = RGBColor(0x1F, 0x29, 0x37)
    r2 = p_call.add_run(
        "Your iOS app acts as a Controller that registers rules with Apple's operating system kernel "
        "(Screen Time subsystem). Once registered, iOS itself enforces the blocks, renders the unskippable shields, "
        "and manages the timers across reboots—even if the app is terminated."
    )
    r2.font.color.rgb = RGBColor(0x37, 0x41, 0x51)
    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # 2. The 4 Pillars
    h2 = doc.add_heading("2. The 4 Pillars of Apple's Screen Time Architecture", level=1)
    h2.runs[0].font.color.rgb = RGBColor(0x11, 0x18, 0x27)
    
    # Pillar 1
    p = doc.add_paragraph()
    r = p.add_run("1. FamilyControls (Authorization & Privacy-Safe Selection)\n")
    r.bold = True
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(0x25, 0x63, 0xEB)
    p.add_run(
        "• Authorization: App requests FaceID/TouchID permission via AuthorizationCenter.shared.requestAuthorization(for: .individual).\n"
        "• Privacy Picker: Apple presents FamilyActivityPicker. The user selects target apps or entire categories (e.g. All Social, All Entertainment, All Web Browsers).\n"
        "• Security: The app receives opaque tokens (FamilyActivitySelection) rather than raw package names, guaranteeing privacy."
    )
    
    # Pillar 2
    p = doc.add_paragraph()
    r = p.add_run("2. ManagedSettings (The System Shield & Anti-Uninstall Engine)\n")
    r.bold = True
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(0x25, 0x63, 0xEB)
    p.add_run(
        "• Block Apps: store.shield.applications = selection.applicationTokens\n"
        "• Block All Web Traffic: store.shield.webDomainCategories = .all() (completely shuts down Safari & WebKit internet access).\n"
        "• Anti-Uninstall Lock: store.application.denyAppRemoval = true (iOS physically removes the 'Delete App' button from the home screen while active!)."
    )

    # Pillar 3
    p = doc.add_paragraph()
    r = p.add_run("3. ShieldConfiguration & ShieldAction (Custom UI & Zero Bypass)\n")
    r.bold = True
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(0x25, 0x63, 0xEB)
    p.add_run(
        "• System Shield: When a blocked app is tapped, iOS replaces the app screen with a full-screen system overlay.\n"
        "• Custom Branding: ShieldConfigurationExtension provides custom title ('Analog Anchor Active'), body copy, icon, and colors.\n"
        "• Zero Bypass: By disabling the dismiss action in ShieldActionExtension, the user has zero bypass mechanisms."
    )

    # Pillar 4
    p = doc.add_paragraph()
    r = p.add_run("4. DeviceActivity (Reboot-Proof Timer Engine)\n")
    r.bold = True
    r.font.size = Pt(11)
    r.font.color.rgb = RGBColor(0x25, 0x63, 0xEB)
    p.add_run(
        "• Native Scheduling: App creates a DeviceActivitySchedule (e.g. 2 hours) and registers it with DeviceActivityCenter.\n"
        "• Survives Reboots: The timer is tracked inside the iOS kernel. If the iPhone restarts, iOS verifies the active schedule on boot and keeps the phone locked.\n"
        "• Automatic Unlock: When time expires, iOS wakes DeviceActivityMonitorExtension to clear the store and unlock the device."
    )
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # 3. Comparison Table
    h3 = doc.add_heading("3. Why the 'No VPN' Approach is Superior on iOS", level=1)
    h3.runs[0].font.color.rgb = RGBColor(0x11, 0x18, 0x27)
    
    table_data = [
        ("Feature / Metric", "Legacy VPN Approach", "Native Screen Time Approach"),
        ("Battery Consumption", "Drains battery routing dummy packets", "0% extra battery (handled at OS level)"),
        ("Bypass Vulnerability", "User can toggle off in Settings > VPN", "Unskippable System Shield + denyAppRemoval"),
        ("Reboot Handling", "Requires reconnect loops / boot tricks", "Native OS persistence across reboots"),
        ("App Store Review", "High scrutiny under VPN Guideline 5.4", "Officially endorsed for digital wellbeing"),
        ("User Experience", "Persistent VPN icon in status bar", "Clean, polished Apple-native UI over blocked apps")
    ]
    
    t = doc.add_table(rows=len(table_data), cols=3)
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    for row_idx, row in enumerate(table_data):
        for col_idx, text in enumerate(row):
            cell = t.cell(row_idx, col_idx)
            cell.text = text
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            p = cell.paragraphs[0]
            if row_idx == 0:
                set_cell_background(cell, "1F2937") # Dark header
                p.runs[0].font.bold = True
                p.runs[0].font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
            else:
                if row_idx % 2 == 1:
                    set_cell_background(cell, "F9FAFB")
                else:
                    set_cell_background(cell, "FFFFFF")
                p.runs[0].font.size = Pt(9.5)
                p.runs[0].font.color.rgb = RGBColor(0x37, 0x41, 0x51)
                
    doc.add_paragraph().paragraph_format.space_after = Pt(12)

    # 4. Project Structure
    h4 = doc.add_heading("4. Recommended Xcode Project Structure", level=1)
    h4.runs[0].font.color.rgb = RGBColor(0x11, 0x18, 0x27)
    
    p_code = doc.add_paragraph()
    p_code.paragraph_format.left_indent = Inches(0.2)
    p_code_run = p_code.add_run(
        "AnalogAnchor-iOS/\n"
        "├── AnalogAnchorApp/               # Main iOS App Target (SwiftUI)\n"
        "│   ├── App/ (AnalogAnchorApp.swift)\n"
        "│   ├── Views/ (HomeView.swift, ChallengeActiveView.swift, AppPickerSheet.swift)\n"
        "│   ├── Services/ (ScreenTimeManager.swift - Coordinates shields & schedules)\n"
        "│   └── Shared/ (App Group for shared state via UserDefaults)\n"
        "│\n"
        "├── DeviceActivityMonitor/         # Extension 1: Background Timer Engine\n"
        "│   └── DeviceActivityMonitorExtension.swift (Handles interval start/end)\n"
        "│\n"
        "├── ShieldConfiguration/           # Extension 2: Custom Shield UI\n"
        "│   └── ShieldConfigurationExtension.swift (Custom text, icons, colors)\n"
        "│\n"
        "└── ShieldAction/                  # Extension 3: Shield Button Handler\n"
        "    └── ShieldActionExtension.swift (Handles user taps on the shield)\n"
    )
    p_code_run.font.name = 'Consolas'
    p_code_run.font.size = Pt(9)
    p_code_run.font.color.rgb = RGBColor(0x1F, 0x29, 0x37)

    # 5. Next Steps
    h5 = doc.add_heading("5. Prerequisites for iOS Development", level=1)
    h5.runs[0].font.color.rgb = RGBColor(0x11, 0x18, 0x27)
    
    doc.add_paragraph(
        "1. Mac computer running macOS with Xcode installed.\n"
        "2. Apple Developer Program Account (Individual or Organization).\n"
        "3. FamilyControls Capability enabled in your Apple Developer Portal.\n"
        "4. App Group configured in Signing & Capabilities to share timer data across targets."
    )
    
    doc.save(filename)
    print(f"Word Document saved: {filename}")


# -------------------------------------------------------------
# 2. GENERATE PDF DOCUMENT (REPORTLAB)
# -------------------------------------------------------------
def create_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=40,
        bottomMargin=40
    )
    
    styles = getSampleStyleSheet()
    
    # Custom styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#1F2937')
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=11,
        leading=15,
        textColor=colors.HexColor('#4B5563')
    )
    
    h1_style = ParagraphStyle(
        'DocH1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor('#111827'),
        spaceBefore=12,
        spaceAfter=6
    )
    
    body_style = ParagraphStyle(
        'DocBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=14,
        textColor=colors.HexColor('#374151'),
        spaceAfter=6
    )
    
    bullet_title = ParagraphStyle(
        'DocBulletTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=13,
        textColor=colors.HexColor('#2563EB'),
        spaceBefore=4
    )
    
    callout_style = ParagraphStyle(
        'DocCallout',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=14,
        textColor=colors.HexColor('#1F2937')
    )
    
    code_style = ParagraphStyle(
        'DocCode',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=8,
        leading=11,
        textColor=colors.HexColor('#1F2937')
    )
    
    table_header = ParagraphStyle(
        'DocTableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=11,
        textColor=colors.white
    )
    
    table_cell = ParagraphStyle(
        'DocTableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor('#374151')
    )
    
    story = []
    
    # Header
    story.append(Paragraph("Analog Anchor: iOS Architecture Guide", title_style))
    story.append(Spacer(1, 4))
    story.append(Paragraph("Building Reboot-Proof Offline Challenges & Focus Modes on Apple iOS (No VPN Needed)", subtitle_style))
    story.append(Spacer(1, 8))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#D1D5DB'), spaceBefore=4, spaceAfter=10))
    
    # 1. Mindset Shift
    story.append(Paragraph("1. The Mindset Shift: Android vs. iOS", h1_style))
    story.append(Paragraph(
        "On Android, an offline challenge app acts like a security guard constantly running in the background, managing dummy VPN routing and listening to boot/unlock broadcasts. On iOS, third-party apps cannot sit in the background or monitor other apps. Instead, iOS utilizes an <b>Orchestrator Model</b>:",
        body_style
    ))
    
    # Callout Box
    callout_text = "<b>Key Principle:</b> Your iOS app acts as a Controller that registers rules with Apple's operating system kernel (Screen Time subsystem). Once registered, iOS itself enforces the blocks, renders the unskippable shields, and manages the timers across reboots—even if the app is terminated."
    callout_table = Table([[Paragraph(callout_text, callout_style)]], colWidths=[530])
    callout_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F3F4F6')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#E5E7EB')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
        ('RIGHTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(callout_table)
    story.append(Spacer(1, 8))
    
    # 2. The 4 Pillars
    story.append(Paragraph("2. The 4 Pillars of Apple's Screen Time Suite", h1_style))
    
    story.append(Paragraph("1. FamilyControls (Authorization & Privacy-Safe Selection)", bullet_title))
    story.append(Paragraph("• <b>Authorization:</b> App requests FaceID/TouchID permission via <code>AuthorizationCenter.shared.requestAuthorization(for: .individual)</code>.<br/>• <b>Privacy Picker:</b> Apple presents <code>FamilyActivityPicker</code>. The user selects target apps or categories (e.g. All Social, All Entertainment, All Web Browsers).<br/>• <b>Security:</b> The app receives opaque tokens (<code>FamilyActivitySelection</code>) rather than raw package names.", body_style))
    
    story.append(Paragraph("2. ManagedSettings (The System Shield & Anti-Uninstall Engine)", bullet_title))
    story.append(Paragraph("• <b>Block Apps:</b> <code>store.shield.applications = selection.applicationTokens</code><br/>• <b>Block All Web Traffic:</b> <code>store.shield.webDomainCategories = .all()</code> (completely shuts down Safari & WebKit internet browsing).<br/>• <b>Anti-Uninstall Lock:</b> <code>store.application.denyAppRemoval = true</code> (iOS physically removes the 'Delete App' button from the home screen while active!).", body_style))
    
    story.append(Paragraph("3. ShieldConfiguration & ShieldAction (Custom UI & Zero Bypass)", bullet_title))
    story.append(Paragraph("• <b>System Shield:</b> When a blocked app is opened, iOS replaces the screen with a full-screen system overlay.<br/>• <b>Custom Branding:</b> <code>ShieldConfigurationExtension</code> provides custom title ('Analog Anchor Active'), body copy, icon, and colors.<br/>• <b>Zero Bypass:</b> By disabling the dismiss action in <code>ShieldActionExtension</code>, the user has zero bypass options.", body_style))

    story.append(Paragraph("4. DeviceActivity (Reboot-Proof Timer Engine)", bullet_title))
    story.append(Paragraph("• <b>Native Scheduling:</b> App registers a <code>DeviceActivitySchedule</code> (e.g. 2 hours) with <code>DeviceActivityCenter</code>.<br/>• <b>Survives Reboots:</b> Timer is tracked inside the iOS kernel. If the iPhone restarts, iOS verifies the active schedule on boot and keeps the phone locked.<br/>• <b>Automatic Unlock:</b> When time expires, iOS wakes <code>DeviceActivityMonitorExtension</code> to clear the store and unlock.", body_style))
    
    story.append(Spacer(1, 8))
    
    # 3. Comparison Table
    story.append(Paragraph("3. Why the 'No VPN' Approach is Superior on iOS", h1_style))
    
    raw_table = [
        [Paragraph("Feature / Metric", table_header), Paragraph("Legacy VPN Approach", table_header), Paragraph("Native Screen Time Approach", table_header)],
        [Paragraph("<b>Battery Consumption</b>", table_cell), Paragraph("Drains battery routing dummy packets", table_cell), Paragraph("<b>0% extra battery</b> (handled at OS level)", table_cell)],
        [Paragraph("<b>Bypass Vulnerability</b>", table_cell), Paragraph("User can toggle off in Settings > VPN", table_cell), Paragraph("<b>Unskippable Shield</b> + denyAppRemoval", table_cell)],
        [Paragraph("<b>Reboot Handling</b>", table_cell), Paragraph("Requires reconnect loops / boot tricks", table_cell), Paragraph("<b>Native OS persistence</b> across reboots", table_cell)],
        [Paragraph("<b>App Store Review</b>", table_cell), Paragraph("High scrutiny under VPN Guideline 5.4", table_cell), Paragraph("<b>Officially endorsed</b> for digital wellbeing", table_cell)],
        [Paragraph("<b>User Experience</b>", table_cell), Paragraph("Persistent VPN icon in status bar", table_cell), Paragraph("<b>Clean, polished Apple-native UI</b>", table_cell)],
    ]
    
    comp_table = Table(raw_table, colWidths=[130, 200, 200])
    comp_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#1F2937')),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E5E7EB')),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.HexColor('#F9FAFB'), colors.white]),
    ]))
    story.append(comp_table)
    story.append(Spacer(1, 10))
    
    # 4. Project Structure
    story.append(Paragraph("4. Recommended Xcode Project Structure", h1_style))
    struct_text = """AnalogAnchor-iOS/
├── AnalogAnchorApp/               # Main iOS App Target (SwiftUI)
│   ├── App/ (AnalogAnchorApp.swift)
│   ├── Views/ (HomeView.swift, ChallengeActiveView.swift, AppPickerSheet.swift)
│   ├── Services/ (ScreenTimeManager.swift - Coordinates shields & schedules)
│   └── Shared/ (App Group for shared state via UserDefaults)
│
├── DeviceActivityMonitor/         # Extension 1: Background Timer Engine
│   └── DeviceActivityMonitorExtension.swift (Handles interval start/end)
│
├── ShieldConfiguration/           # Extension 2: Custom Shield UI
│   └── ShieldConfigurationExtension.swift (Custom text, icons, colors)
│
└── ShieldAction/                  # Extension 3: Shield Button Handler
    └── ShieldActionExtension.swift (Handles user taps on the shield)"""
    
    struct_table = Table([[Paragraph(struct_text.replace("\n", "<br/>").replace(" ", "&nbsp;"), code_style)]], colWidths=[530])
    struct_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('LEFTPADDING', (0,0), (-1,-1), 10),
        ('RIGHTPADDING', (0,0), (-1,-1), 10),
    ]))
    story.append(struct_table)
    
    # 5. Prerequisites
    story.append(Paragraph("5. Prerequisites for iOS Development", h1_style))
    story.append(Paragraph(
        "1. <b>Mac computer</b> running macOS with <b>Xcode</b> installed.<br/>"
        "2. <b>Apple Developer Program Account</b> (Individual or Organization).<br/>"
        "3. <b>FamilyControls Capability</b> enabled in your Apple Developer Portal.<br/>"
        "4. <b>App Group</b> configured in Signing & Capabilities to share timer data across targets.",
        body_style
    ))
    
    doc.build(story)
    print(f"PDF Document saved: {filename}")

if __name__ == '__main__':
    docx_path = "Analog_Anchor_iOS_Architecture_Guide.docx"
    pdf_path = "Analog_Anchor_iOS_Architecture_Guide.pdf"
    create_docx(docx_path)
    create_pdf(pdf_path)
