import os
import sys
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

from reportlab.lib.pagesizes import letter
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors

# ---------------------------------------------------------------------------
# XML Helper Functions for DOCX styling
# ---------------------------------------------------------------------------
def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=120, bottom=120, left=160, right=160):
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

def set_table_borders(table, color="CBD5E1", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'  <w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'  <w:left w:val="none"/>'
        f'  <w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'  <w:right w:val="none"/>'
        f'  <w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'  <w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

def set_callout_box_borders(cell, border_color="0284C7", bg_color="F0F9FF"):
    set_cell_background(cell, bg_color)
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'  <w:top w:val="none"/>'
        f'  <w:left w:val="single" w:sz="24" w:space="0" w:color="{border_color}"/>'
        f'  <w:bottom w:val="none"/>'
        f'  <w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)
    set_cell_margins(cell, top=140, bottom=140, left=200, right=180)

def add_page_number_to_run(run):
    fldChar1 = parse_xml(r'<w:fldChar %s w:fldCharType="begin"/>' % nsdecls('w'))
    instrText = parse_xml(r'<w:instrText %s xml:space="preserve"> PAGE </w:instrText>' % nsdecls('w'))
    fldChar2 = parse_xml(r'<w:fldChar %s w:fldCharType="separate"/>' % nsdecls('w'))
    fldChar3 = parse_xml(r'<w:fldChar %s w:fldCharType="end"/>' % nsdecls('w'))
    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    run._r.append(fldChar3)

def add_numpages_to_run(run):
    fldChar1 = parse_xml(r'<w:fldChar %s w:fldCharType="begin"/>' % nsdecls('w'))
    instrText = parse_xml(r'<w:instrText %s xml:space="preserve"> NUMPAGES </w:instrText>' % nsdecls('w'))
    fldChar2 = parse_xml(r'<w:fldChar %s w:fldCharType="separate"/>' % nsdecls('w'))
    fldChar3 = parse_xml(r'<w:fldChar %s w:fldCharType="end"/>' % nsdecls('w'))
    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    run._r.append(fldChar3)

# ---------------------------------------------------------------------------
# DOCX Generation Function
# ---------------------------------------------------------------------------
def generate_docx(filepath):
    doc = Document()
    
    # 0.75" Margins for clean printing
    for section in doc.sections:
        section.top_margin = Inches(0.75)
        section.bottom_margin = Inches(0.75)
        section.left_margin = Inches(0.75)
        section.right_margin = Inches(0.75)
        
        # Header setup
        header = section.header
        p_hdr = header.paragraphs[0]
        p_hdr.text = "Analog Anchor • Statement of Work & Technical Specification (iOS)"
        p_hdr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p_hdr.runs[0].font.name = "Calibri"
        p_hdr.runs[0].font.size = Pt(8.5)
        p_hdr.runs[0].font.color.rgb = RGBColor(0x94, 0xA3, 0xB8)
        
        # Footer setup
        footer = section.footer
        p_ftr = footer.paragraphs[0]
        p_ftr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        r_ftr_lbl = p_ftr.add_run("Confidential — For Procurement & Engineering Verification Only           Page ")
        r_ftr_lbl.font.name = "Calibri"
        r_ftr_lbl.font.size = Pt(8.5)
        r_ftr_lbl.font.color.rgb = RGBColor(0x94, 0xA3, 0xB8)
        
        r_pg = p_ftr.add_run()
        r_pg.font.name = "Calibri"
        r_pg.font.size = Pt(8.5)
        r_pg.font.color.rgb = RGBColor(0x64, 0x74, 0x8B)
        add_page_number_to_run(r_pg)
        
        r_of = p_ftr.add_run(" of ")
        r_of.font.name = "Calibri"
        r_of.font.size = Pt(8.5)
        r_of.font.color.rgb = RGBColor(0x94, 0xA3, 0xB8)
        
        r_tot = p_ftr.add_run()
        r_tot.font.name = "Calibri"
        r_tot.font.size = Pt(8.5)
        r_tot.font.color.rgb = RGBColor(0x64, 0x74, 0x8B)
        add_numpages_to_run(r_tot)

    # Styles setup
    styles = doc.styles
    normal_style = styles['Normal']
    normal_style.font.name = 'Calibri'
    normal_style.font.size = Pt(10)
    normal_style.font.color.rgb = RGBColor(0x33, 0x41, 0x55) # Slate 700
    
    # -------------------------------------------------------------
    # Document Header Title Block
    # -------------------------------------------------------------
    p_badge = doc.add_paragraph()
    p_badge.paragraph_format.space_before = Pt(0)
    p_badge.paragraph_format.space_after = Pt(4)
    r_badge = p_badge.add_run("ENGINEERING & PROCUREMENT SPECIFICATION | iOS NATIVE")
    r_badge.font.name = 'Calibri'
    r_badge.font.size = Pt(9)
    r_badge.font.bold = True
    r_badge.font.color.rgb = RGBColor(0x02, 0x84, 0xC7) # Sky 600

    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(0)
    p_title.paragraph_format.space_after = Pt(2)
    r_title = p_title.add_run("Statement of Work & Technical Specification")
    r_title.font.name = 'Calibri'
    r_title.font.size = Pt(22)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14) # Obsidian

    p_sub = doc.add_paragraph()
    p_sub.paragraph_format.space_before = Pt(0)
    p_sub.paragraph_format.space_after = Pt(12)
    r_sub = p_sub.add_run("Analog Anchor \"Offline Challenge\" (iOS 16.0+ Standalone SwiftUI)")
    r_sub.font.name = 'Calibri'
    r_sub.font.size = Pt(12)
    r_sub.font.bold = True
    r_sub.font.color.rgb = RGBColor(0x47, 0x55, 0x69)

    # Document Control Metadata Box
    meta_table = doc.add_table(rows=3, cols=2)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(meta_table, color="CBD5E1", sz="4", val="single")
    
    metadata = [
        [("Project / Client:", "Analog Anchor (Digital Wellbeing Ecosystem)"),
         ("Document Version:", "1.0 — Final Procurement & Architecture Spec")],
        [("Target Platform:", "Apple iOS 16.0+ (SwiftUI, Screen Time Frameworks)"),
         ("Functional Baseline:", "Android App: com.analoganchor.offlinechallenge")],
        [("Acceptance Standard:", "Milestone-Based Payout on Physical iPhone Verification"),
         ("Key Frameworks:", "FamilyControls • ManagedSettings • DeviceActivity")]
    ]
    
    for row_idx, row_content in enumerate(metadata):
        for col_idx, (label, val) in enumerate(row_content):
            cell = meta_table.cell(row_idx, col_idx)
            set_cell_background(cell, "F8FAFC" if row_idx % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, top=60, bottom=60, left=120, right=120)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            
            r_lbl = p.add_run(f"{label} ")
            r_lbl.font.bold = True
            r_lbl.font.size = Pt(8.5)
            r_lbl.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)
            
            r_v = p.add_run(val)
            r_v.font.size = Pt(8.5)
            r_v.font.color.rgb = RGBColor(0x47, 0x55, 0x69)
            
    doc.add_paragraph().paragraph_format.space_after = Pt(10)

    # -------------------------------------------------------------
    # SECTION 1: Project Overview & Objective
    # -------------------------------------------------------------
    h1 = doc.add_heading(level=1)
    h1.paragraph_format.space_before = Pt(14)
    h1.paragraph_format.space_after = Pt(4)
    r = h1.add_run("1. Project Overview & Objective")
    r.font.name = 'Calibri'
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14)

    p1 = doc.add_paragraph()
    p1.paragraph_format.space_after = Pt(6)
    p1.add_run(
        "Build a standalone iOS native application (SwiftUI / iOS 16.0+) called \"Analog Anchor Offline Challenge\". "
        "The application is a digital detox / focus tool designed to temporarily shield user-selected distracting apps "
        "during a committed offline period (2m test, 12h, 18h, 36h, 72h). "
        "The functional baseline, branding, and behavioral philosophy are derived directly from the existing Android app ("
    )
    r_code = p1.add_run("com.analoganchor.offlinechallenge")
    r_code.font.name = "Consolas"
    r_code.font.size = Pt(9)
    p1.add_run(").")

    # Callout: Core Philosophical & Architectural Baseline
    callout_tbl = doc.add_table(rows=1, cols=1)
    callout_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    c_cell = callout_tbl.cell(0, 0)
    set_callout_box_borders(c_cell, border_color="0284C7", bg_color="F0F9FF")
    p_c = c_cell.paragraphs[0]
    p_c.paragraph_format.space_after = Pt(0)
    r_c_bold = p_c.add_run("Core Design & Operating Philosophy: ")
    r_c_bold.bold = True
    r_c_bold.font.color.rgb = RGBColor(0x03, 0x69, 0xA1) # Sky 700
    r_c_text = p_c.add_run(
        "Unlike permissive blockers that offer easy \"pause\" or \"snooze\" buttons, Analog Anchor enforces true "
        "pre-commitment. Once a challenge begins, the selected distracting applications are shielded at the iOS kernel level. "
        "The only early exit path is an Emergency Token entered by a trusted partner or founder, preventing compulsive digital relapse."
    )
    r_c_text.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # -------------------------------------------------------------
    # SECTION 2: Procurement Roles & Acceptance Governance
    # -------------------------------------------------------------
    h2 = doc.add_heading(level=1)
    h2.paragraph_format.space_before = Pt(12)
    h2.paragraph_format.space_after = Pt(4)
    r = h2.add_run("2. Procurement Roles & Acceptance Governance")
    r.font.name = 'Calibri'
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14)

    roles = [
        ("Client / IP Owner: ", "Analog Anchor holds all code repository rights, App IDs, and Apple Developer Account access. All source code, assets, and documentation are proprietary work-for-hire deliverables."),
        ("Deliverable Standard: ", "Milestone-based payout upon physical device verification (Acceptance Testing). No milestone will be considered completed without demonstration on physical iOS 16+ hardware."),
        ("Core Engine: ", "Apple Screen Time Frameworks (FamilyControls, ManagedSettings, DeviceActivity). No third-party tracking, no VPN tunnels, and zero unauthorized analytics.")
    ]
    for b_title, b_desc in roles:
        p_b = doc.add_paragraph(style='List Bullet')
        p_b.paragraph_format.space_before = Pt(2)
        p_b.paragraph_format.space_after = Pt(3)
        rb = p_b.add_run(b_title)
        rb.bold = True
        rb.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
        p_b.add_run(b_desc)

    # Milestone Breakdown Table
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    p_tbl_lbl = doc.add_paragraph()
    p_tbl_lbl.paragraph_format.space_after = Pt(3)
    r_tbl_lbl = p_tbl_lbl.add_run("Table 2.1: Procurement Milestone & Payout Schedule")
    r_tbl_lbl.bold = True
    r_tbl_lbl.font.size = Pt(9.5)
    r_tbl_lbl.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)

    m_table = doc.add_table(rows=5, cols=4)
    m_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(m_table, color="CBD5E1", sz="4", val="single")

    m_headers = ["Milestone", "Scope / Deliverables", "Target Frameworks", "Acceptance Verification"]
    m_data = [
        ("M1: Foundation & Screen Time Auth", 
         "Xcode project architecture, Obsidian/Cyan UI theme, AuthorizationCenter entitlement integration", 
         "FamilyControls, SwiftUI", 
         "Individual authorization prompt triggers FaceID/Passcode successfully on physical device"),
        ("M2: Shield Engine & Active Countdown", 
         "SetupView, AppPickerSheet, ChallengeView with circular progress indicator and live timer", 
         "ManagedSettings, FamilyActivityPicker", 
         "Selected apps immediately display native system shield upon challenge engagement"),
        ("M3: Crypto Vault & Emergency Bypass", 
         "PinVault (XOR salt 0x5A) and TokenDecoder ported from Kotlin, EmergencyUnlockModal", 
         "CryptoKit, Foundation", 
         "Valid tokens generated from decoy-tool.html decode PINs (5222, 2555, 3555) and release shields"),
        ("M4: Completion, Polish & Handover", 
         "CompletionView celebration screen, automatic shield teardown, outbound ecosystem CTA link", 
         "DeviceActivity, StoreKit", 
         "Clean 2-minute end-to-end challenge lifecycle test, zero memory leaks, code delivery to repo")
    ]

    for col_idx, h_text in enumerate(m_headers):
        cell = m_table.cell(0, col_idx)
        set_cell_background(cell, "0F172A")
        set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
        p = cell.paragraphs[0]
        r = p.add_run(h_text)
        r.bold = True
        r.font.size = Pt(8.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    for row_idx, row_content in enumerate(m_data, start=1):
        bg = "F8FAFC" if row_idx % 2 == 1 else "FFFFFF"
        for col_idx, text in enumerate(row_content):
            cell = m_table.cell(row_idx, col_idx)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=70, bottom=70, left=100, right=100)
            p = cell.paragraphs[0]
            r = p.add_run(text)
            r.font.size = Pt(8)
            if col_idx == 0:
                r.bold = True
                r.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
            else:
                r.font.color.rgb = RGBColor(0x33, 0x41, 0x55)

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # -------------------------------------------------------------
    # SECTION 3: Screen Architecture & User Flow (The 3 Core Screens)
    # -------------------------------------------------------------
    h3 = doc.add_heading(level=1)
    h3.paragraph_format.space_before = Pt(12)
    h3.paragraph_format.space_after = Pt(4)
    r = h3.add_run("3. Screen Architecture & User Flow (The 3 Core Screens)")
    r.font.name = 'Calibri'
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14)

    # Screen 1
    h3_1 = doc.add_heading(level=2)
    h3_1.paragraph_format.space_before = Pt(8)
    h3_1.paragraph_format.space_after = Pt(3)
    r = h3_1.add_run("Screen 1: Setup & Pre-Commitment (SetupView.swift)")
    r.font.name = 'Calibri'
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x02, 0x84, 0xC7)

    s1_items = [
        ("1. Header Hero:", "Displays app title \"Analog Anchor Offline Challenge\" accented in Cyan Glow (#00E5FF) against an Obsidian (#0A0E14) deep background. Subtitle: \"Choose intentional presence over subconscious screen consumption.\""),
        ("2. Attention Philosophy Card (Pre-Commitment):", "Educational card emphasizing that continuous digital connectivity erodes focus and presence. Features an interactive link: \"After completing this challenge, the main Analog Anchor app helps you build conscious control ↗\" linking directly to https://get-analog-anchor.com/."),
        ("3. App Selection Trigger:", "Action button \"Select Shielded Apps\" presenting Apple's native FamilyActivityPicker(selection: $model.activitySelection). The selected application tokens are persisted to shared UserDefaults(suiteName: \"group.com.analoganchor.offlinechallenge\")."),
        ("4. Duration Selection (Pre-Commitment Matrix):", "Presents 5 pre-commitment duration buttons: 2 Minutes (Test Mode - accented in Amber #FFB300 / #D97706), 12 Hours, 18 Hours, 36 Hours, and 72 Hours. Tapping any duration immediately transitions to Screen 2 and engages the kernel shield.")
    ]
    for title, desc in s1_items:
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        r_t = p.add_run(f"• {title} ")
        r_t.bold = True
        r_t.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
        p.add_run(desc)

    # Screen 2
    h3_2 = doc.add_heading(level=2)
    h3_2.paragraph_format.space_before = Pt(8)
    h3_2.paragraph_format.space_after = Pt(3)
    r = h3_2.add_run("Screen 2: Active Challenge & Countdown (ChallengeView.swift)")
    r.font.name = 'Calibri'
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x02, 0x84, 0xC7)

    s2_items = [
        ("1. Circular Progress Indicator:", "Animated circular gauge displaying remaining time formatted as XXh YYm ZZs along with the real-time percentage completed, ticking down every second."),
        ("2. System Shield Enforcement:", "Immediately upon session initialization, ManagedSettingsStore().shield.applications is assigned the active selection tokens. When a user taps a blocked application (e.g. Instagram, YouTube, Safari), iOS presents the native full-screen shield overlay."),
        ("3. Emergency Bypass / Token Unlock Modal:", "No simple \"Cancel\" or \"Give Up\" button is provided. To abort early, the user must tap \"Emergency Unlock\" and input an authorized token issued by the founder/accountability partner (e.g. AA-ANCHOR-5222...). The token is validated via native Swift TokenDecoder against PinVault (PIN 5222 for Request 1, 2555 for Request 2, 3555 for Request 3). On successful verification, ManagedSettingsStore().clearAllSettings() unshields all apps and the session terminates cleanly.")
    ]
    for title, desc in s2_items:
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        r_t = p.add_run(f"• {title} ")
        r_t.bold = True
        r_t.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
        p.add_run(desc)

    # Screen 3
    h3_3 = doc.add_heading(level=2)
    h3_3.paragraph_format.space_before = Pt(8)
    h3_3.paragraph_format.space_after = Pt(3)
    r = h3_3.add_run("Screen 3: Completion & Barakah (CompletionView.swift)")
    r.font.name = 'Calibri'
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x02, 0x84, 0xC7)

    s3_items = [
        ("1. Automatic Lifecycle Transition:", "Triggered automatically the instant the challenge countdown timer reaches zero (or via DeviceActivity scheduling callback)."),
        ("2. Celebratory & Affirmation UI:", "Presents celebratory badge and mindfulness affirmation: \"Challenge Completed! You have reclaimed your presence and fortified your intentionality.\""),
        ("3. Automatic Shield Teardown:", "Automatically invokes ManagedSettingsStore().clearAllSettings(), releasing all application restrictions with zero manual user effort."),
        ("4. Ecosystem Conversion CTA:", "High-visibility Call-to-Action button: \"Discover the Main Analog Anchor Ecosystem ↗\" redirecting to https://get-analog-anchor.com/ to transition the user into long-term conscious habits.")
    ]
    for title, desc in s3_items:
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        r_t = p.add_run(f"• {title} ")
        r_t.bold = True
        r_t.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
        p.add_run(desc)

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # -------------------------------------------------------------
    # SECTION 4: Technical Specifications & File Structure
    # -------------------------------------------------------------
    h4 = doc.add_heading(level=1)
    h4.paragraph_format.space_before = Pt(12)
    h4.paragraph_format.space_after = Pt(4)
    r = h4.add_run("4. Technical Specifications & File Structure")
    r.font.name = 'Calibri'
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14)

    p4_intro = doc.add_paragraph()
    p4_intro.paragraph_format.space_after = Pt(4)
    p4_intro.add_run(
        "The project is structured as a pure SwiftUI native iOS application targeting iOS 16.0+ with zero external package dependencies. "
        "The complete source directory architecture is specified as follows:"
    )

    # Monospace Code Box for File Tree
    tree_tbl = doc.add_table(rows=1, cols=1)
    tree_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    t_cell = tree_tbl.cell(0, 0)
    set_cell_background(t_cell, "F8FAFC")
    tcPr = t_cell._tc.get_or_add_tcPr()
    borders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'  <w:top w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
        f'  <w:left w:val="single" w:sz="16" w:space="0" w:color="0284C7"/>'
        f'  <w:bottom w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
        f'  <w:right w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(borders)
    set_cell_margins(t_cell, top=100, bottom=100, left=140, right=140)
    
    file_tree = (
        "AnalogAnchorOfflineChallenge/\n"
        "├── App/\n"
        "│   ├── OfflineChallengeApp.swift   // App entry point + FamilyControls requestAuthorization\n"
        "│   └── AppTheme.swift              // Obsidian (#0A0E14), CyanGlow (#00E5FF), Amber (#FFB300)\n"
        "├── Features/\n"
        "│   ├── Setup/\n"
        "│   │   ├── SetupView.swift          // Pre-commitment card + Duration triggers\n"
        "│   │   └── AppPickerSheet.swift     // FamilyActivityPicker integration\n"
        "│   ├── Challenge/\n"
        "│   │   ├── ChallengeView.swift      // Live circular countdown + remaining timer\n"
        "│   │   └── EmergencyUnlockModal.swift // Token input field + TokenDecoder verification\n"
        "│   └── Completion/\n"
        "│       └── CompletionView.swift     // Celebration screen + Ecosystem link\n"
        "├── ScreenTime/\n"
        "│   ├── ScreenTimeManager.swift     // AuthorizationCenter + ManagedSettingsStore controller\n"
        "│   └── Model/\n"
        "│       └── ChallengeSession.swift  // Active session state, start time, target duration\n"
        "└── Crypto/\n"
        "    ├── PinVault.swift              // XOR salt 0x5A, verifying PINs 5222, 2555, 3555\n"
        "    └── TokenDecoder.swift          // NFKC Unicode normalization, cipher mapping, checksum"
    )
    p_tree = t_cell.paragraphs[0]
    p_tree.paragraph_format.space_after = Pt(0)
    r_tree = p_tree.add_run(file_tree)
    r_tree.font.name = "Consolas"
    r_tree.font.size = Pt(8.5)
    r_tree.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)

    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # Component Detail Table
    p_tbl2_lbl = doc.add_paragraph()
    p_tbl2_lbl.paragraph_format.space_after = Pt(3)
    r_tbl2_lbl = p_tbl2_lbl.add_run("Table 4.1: Source Component Functional Specifications")
    r_tbl2_lbl.bold = True
    r_tbl2_lbl.font.size = Pt(9.5)
    r_tbl2_lbl.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)

    c_table = doc.add_table(rows=8, cols=3)
    c_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(c_table, color="CBD5E1", sz="4", val="single")

    c_headers = ["Component File", "Architectural Role", "Key Technical Implementation Details"]
    c_data = [
        ("OfflineChallengeApp.swift", "App Lifecycle & Root", "Requests AuthorizationCenter.shared.requestAuthorization(for: .individual) on initial launch. Manages NavigationStack root depending on active session state."),
        ("AppTheme.swift", "Design System / Tokens", "Defines brand colors: Obsidian Background (#0A0E14), Cyan Glow (#00E5FF), Amber Accent (#FFB300), Slate Card (#161F2E), and custom button styles."),
        ("SetupView.swift & AppPickerSheet.swift", "Pre-Commitment & Config", "Displays philosophy card, presents FamilyActivityPicker modal, persists opaque application tokens to App Group UserDefaults, and triggers session starts."),
        ("ChallengeView.swift", "Active Shield Experience", "Renders 1-second interval Timer publishing to Combine, circular stroked progress ring, remaining duration display, and triggers emergency unlock modal."),
        ("EmergencyUnlockModal.swift", "Secure Bypass Gate", "Provides text input with auto-capitalization and aggressive sanitization. Calls TokenDecoder; on success, invokes ScreenTimeManager.shared.unshieldAll()."),
        ("ScreenTimeManager.swift & ChallengeSession.swift", "Screen Time Subsystem", "Wraps ManagedSettingsStore() to atomically set store.shield.applications. ChallengeSession handles persistence across app termination."),
        ("PinVault.swift & TokenDecoder.swift", "Obfuscation & Crypto", "1:1 Swift port of Kotlin logic. XOR byte decoding with salt 0x5A; NFKC Unicode normalization, delimiter removal, 5-round polynomial checksum, and inverse cipher mapping.")
    ]

    for col_idx, h_text in enumerate(c_headers):
        cell = c_table.cell(0, col_idx)
        set_cell_background(cell, "0F172A")
        set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
        p = cell.paragraphs[0]
        r = p.add_run(h_text)
        r.bold = True
        r.font.size = Pt(8.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    for row_idx, row_content in enumerate(c_data, start=1):
        bg = "F8FAFC" if row_idx % 2 == 1 else "FFFFFF"
        for col_idx, text in enumerate(row_content):
            cell = c_table.cell(row_idx, col_idx)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=70, bottom=70, left=100, right=100)
            p = cell.paragraphs[0]
            r = p.add_run(text)
            r.font.size = Pt(8)
            if col_idx == 0:
                r.bold = True
                r.font.name = "Consolas"
                r.font.color.rgb = RGBColor(0x02, 0x84, 0xC7)
            else:
                r.font.color.rgb = RGBColor(0x33, 0x41, 0x55)

    doc.add_paragraph().paragraph_format.space_after = Pt(8)

    # -------------------------------------------------------------
    # SECTION 5: Verification & Acceptance Criteria
    # -------------------------------------------------------------
    h5 = doc.add_heading(level=1)
    h5.paragraph_format.space_before = Pt(12)
    h5.paragraph_format.space_after = Pt(4)
    r = h5.add_run("5. Verification & Acceptance Criteria")
    r.font.name = 'Calibri'
    r.font.size = Pt(13)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14)

    p5_intro = doc.add_paragraph()
    p5_intro.paragraph_format.space_after = Pt(4)
    p5_intro.add_run("Payment milestones and formal delivery sign-off are contingent upon passing all five verification gates on physical hardware:")

    crit_items = [
        ("1. Build & Signing Integrity:", "Must compile cleanly in Xcode 15/16 with zero errors and zero warnings under Swift 5.9+. Must deploy and execute flawlessly on a physical iPhone running iOS 16.0 or later with provisioning profile containing the FamilyControls entitlement."),
        ("2. Screen Time Authorization Flow:", "On initial launch, invoking ScreenTimeManager.shared.requestAuthorization() must present Apple's native Screen Time permission dialogue. Upon user biometric/passcode approval, the app must transition to the authorized state."),
        ("3. Shielding & Enforcement Verification:", "When the user selects target applications (e.g. Instagram, TikTok, YouTube) and engages the 2-minute test challenge (or any standard duration), opening those apps must immediately trigger Apple's system Shield screen. Upon timer expiration, all shields must release automatically without requiring app restart."),
        ("4. Cryptographic Token Decoding & Bypass:", "Entering a valid emergency token generated by decoy-tool.html / support-tool.html (e.g. serial format with valid 5-round checksum or legacy short format) must correctly decode PIN 5222 (Request 1), PIN 2555 (Request 2), or PIN 3555 (Request 3). Upon successful validation, all shields must clear immediately and transition the app to completion."),
        ("5. Persistence & Reboot Resilience:", "Terminating the application from the iOS App Switcher or rebooting the iPhone during an active session must NOT release shields. Relaunching the app must restore the live countdown based on the persisted session start timestamp.")
    ]
    for title, desc in crit_items:
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(3)
        r_t = p.add_run(f"• {title} ")
        r_t.bold = True
        r_t.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
        p.add_run(desc)

    # Table 5.1 Acceptance Testing Checklist
    doc.add_paragraph().paragraph_format.space_after = Pt(2)
    p_tbl3_lbl = doc.add_paragraph()
    p_tbl3_lbl.paragraph_format.space_after = Pt(3)
    r_tbl3_lbl = p_tbl3_lbl.add_run("Table 5.1: Physical Device Acceptance Verification Checklist")
    r_tbl3_lbl.bold = True
    r_tbl3_lbl.font.size = Pt(9.5)
    r_tbl3_lbl.font.color.rgb = RGBColor(0x1E, 0x29, 0x3B)

    chk_table = doc.add_table(rows=7, cols=4)
    chk_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(chk_table, color="CBD5E1", sz="4", val="single")

    chk_headers = ["Check", "Acceptance Gate", "Success Verification Requirement", "Status / Sign-off"]
    chk_data = [
        ("[  ]", "Gate 1: Build & Signing", "Compiles in Xcode 15/16 with Swift 5.9+; installs cleanly on physical iOS 16+ iPhone with FamilyControls entitlement", "[  ] Pass   [  ] Fail"),
        ("[  ]", "Gate 2: Screen Time Auth", "Calls AuthorizationCenter.shared.requestAuthorization(for: .individual); FaceID/Passcode prompt grants approval", "[  ] Pass   [  ] Fail"),
        ("[  ]", "Gate 3: Shielding Enforcement", "Selecting target apps in FamilyActivityPicker and engaging 2-minute test challenge immediately triggers native shield", "[  ] Pass   [  ] Fail"),
        ("[  ]", "Gate 4: Crypto Token Bypass", "Inputting valid token decodes PIN (5222, 2555, 3555), invokes ManagedSettingsStore().clearAllSettings(), unshields apps", "[  ] Pass   [  ] Fail"),
        ("[  ]", "Gate 5: Auto Timer Teardown", "Upon countdown reaching 00m 00s, shields clear automatically with zero user action; displays completion screen", "[  ] Pass   [  ] Fail"),
        ("[  ]", "Gate 6: Persistence & Reboot", "Force-closing app from App Switcher or rebooting device preserves shield enforcement; timer resumes on launch", "[  ] Pass   [  ] Fail")
    ]

    for col_idx, h_text in enumerate(chk_headers):
        cell = chk_table.cell(0, col_idx)
        set_cell_background(cell, "0F172A")
        set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
        p = cell.paragraphs[0]
        r = p.add_run(h_text)
        r.bold = True
        r.font.size = Pt(8.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    for row_idx, row_content in enumerate(chk_data, start=1):
        bg = "F8FAFC" if row_idx % 2 == 1 else "FFFFFF"
        for col_idx, text in enumerate(row_content):
            cell = chk_table.cell(row_idx, col_idx)
            set_cell_background(cell, bg)
            set_cell_margins(cell, top=70, bottom=70, left=100, right=100)
            p = cell.paragraphs[0]
            r = p.add_run(text)
            r.font.size = Pt(8)
            if col_idx == 0:
                r.bold = True
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r.font.name = "Consolas"
                r.font.color.rgb = RGBColor(0x02, 0x84, 0xC7)
            elif col_idx == 1:
                r.bold = True
                r.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
            elif col_idx == 3:
                r.bold = True
                r.font.name = "Consolas"
                r.font.color.rgb = RGBColor(0x47, 0x55, 0x69)
            else:
                r.font.color.rgb = RGBColor(0x33, 0x41, 0x55)

    # Acceptance Sign-Off Block
    doc.add_paragraph().paragraph_format.space_after = Pt(12)
    p_sign_lbl = doc.add_paragraph()
    p_sign_lbl.paragraph_format.space_after = Pt(4)
    r_sign_lbl = p_sign_lbl.add_run("Formal Acceptance & Execution Approval")
    r_sign_lbl.bold = True
    r_sign_lbl.font.size = Pt(11)
    r_sign_lbl.font.color.rgb = RGBColor(0x0A, 0x0E, 0x14)

    sign_table = doc.add_table(rows=3, cols=2)
    sign_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(sign_table, color="CBD5E1", sz="4", val="single")

    sign_data = [
        [("Client / IP Owner: Analog Anchor", "Lead iOS Engineer / Contractor:"),
         ("", "")],
        [("Authorized Signatory: __________________________", "Authorized Signatory: __________________________"),
         ("", "")],
        [("Date: ____ / ____ / 2026", "Date: ____ / ____ / 2026"),
         ("", "")]
    ]

    for row_idx, row_content in enumerate(sign_data):
        for col_idx, (line1, line2) in enumerate(row_content):
            cell = sign_table.cell(row_idx, col_idx)
            set_cell_background(cell, "F8FAFC" if row_idx == 0 else "FFFFFF")
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            p = cell.paragraphs[0]
            p.paragraph_format.space_after = Pt(0)
            r = p.add_run(line1)
            r.font.size = Pt(8.5)
            if row_idx == 0:
                r.bold = True
                r.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
            else:
                r.font.color.rgb = RGBColor(0x47, 0x55, 0x69)

    # Save Word document
    doc.save(filepath)
    print(f"DOCX successfully generated at: {filepath}")

# ---------------------------------------------------------------------------
# PDF Generation Function
# ---------------------------------------------------------------------------
def generate_pdf(filepath):
    doc = SimpleDocTemplate(
        filepath,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=40,
        bottomMargin=40
    )
    
    styles = getSampleStyleSheet()
    
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=18,
        leading=22,
        textColor=colors.HexColor('#0A0E14')
    )
    
    badge_style = ParagraphStyle(
        'DocBadge',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor('#0284C7')
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=11,
        leading=14,
        textColor=colors.HexColor('#475569')
    )
    
    h1_style = ParagraphStyle(
        'DocH1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=colors.HexColor('#0A0E14'),
        spaceBefore=10,
        spaceAfter=4
    )

    h2_style = ParagraphStyle(
        'DocH2',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10.5,
        leading=14,
        textColor=colors.HexColor('#0284C7'),
        spaceBefore=6,
        spaceAfter=3
    )
    
    body_style = ParagraphStyle(
        'DocBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12,
        textColor=colors.HexColor('#334155'),
        spaceAfter=4
    )

    code_style = ParagraphStyle(
        'DocCode',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor('#0F172A')
    )

    callout_style = ParagraphStyle(
        'DocCallout',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=12,
        textColor=colors.HexColor('#0F172A')
    )

    tbl_hdr = ParagraphStyle(
        'TblHdr',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=10,
        textColor=colors.white
    )

    tbl_cell = ParagraphStyle(
        'TblCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor('#334155')
    )

    tbl_cell_bold = ParagraphStyle(
        'TblCellB',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=10,
        textColor=colors.HexColor('#0F172A')
    )

    story = []
    
    story.append(Paragraph("ENGINEERING & PROCUREMENT SPECIFICATION | iOS NATIVE", badge_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("Statement of Work & Technical Specification", title_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("Analog Anchor \"Offline Challenge\" (iOS 16.0+ Standalone SwiftUI)", subtitle_style))
    story.append(Spacer(1, 6))
    story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor('#CBD5E1'), spaceBefore=2, spaceAfter=8))

    # Metadata Table
    meta_data = [
        [Paragraph("<b>Project / Client:</b> Analog Anchor", tbl_cell), Paragraph("<b>Document Version:</b> 1.0 — Final SOW & Tech Spec", tbl_cell)],
        [Paragraph("<b>Target Platform:</b> iOS 16.0+ (SwiftUI, Screen Time)", tbl_cell), Paragraph("<b>Functional Baseline:</b> com.analoganchor.offlinechallenge", tbl_cell)],
        [Paragraph("<b>Acceptance:</b> Milestone Payout on Physical iPhone", tbl_cell), Paragraph("<b>Key Frameworks:</b> FamilyControls • ManagedSettings", tbl_cell)]
    ]
    t_meta = Table(meta_data, colWidths=[265, 265])
    t_meta.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(t_meta)
    story.append(Spacer(1, 6))

    # Section 1
    story.append(Paragraph("1. Project Overview & Objective", h1_style))
    story.append(Paragraph(
        "Build a standalone iOS native application (SwiftUI / iOS 16.0+) called \"Analog Anchor Offline Challenge\". "
        "The application is a digital detox / focus tool designed to temporarily shield user-selected distracting apps "
        "during a committed offline period (2m test, 12h, 18h, 36h, 72h). "
        "The functional baseline, branding, and behavioral philosophy are derived directly from the existing Android app (<code>com.analoganchor.offlinechallenge</code>).",
        body_style
    ))
    
    callout_txt = "<b>Core Design Philosophy:</b> Unlike permissive blockers that offer easy \"pause\" buttons, Analog Anchor enforces true pre-commitment. Distracting apps are shielded at the iOS kernel level. The only early exit path is an Emergency Token entered by a partner/founder, preventing digital relapse."
    t_call = Table([[Paragraph(callout_txt, callout_style)]], colWidths=[530])
    t_call.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F0F9FF')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#0284C7')),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_call)
    story.append(Spacer(1, 6))

    # Section 2
    story.append(Paragraph("2. Procurement Roles & Acceptance Governance", h1_style))
    story.append(Paragraph("• <b>Client / IP Owner:</b> Analog Anchor (Holds all code repository, App IDs, and Apple Developer Account access).", body_style))
    story.append(Paragraph("• <b>Deliverable Standard:</b> Milestone-based payout upon physical device verification (Acceptance Testing).", body_style))
    story.append(Paragraph("• <b>Core Engine:</b> Apple Screen Time Frameworks (<code>FamilyControls</code>, <code>ManagedSettings</code>, <code>DeviceActivity</code>).", body_style))

    # Milestone Table
    m_raw = [
        [Paragraph("Milestone", tbl_hdr), Paragraph("Scope / Deliverables", tbl_hdr), Paragraph("Frameworks", tbl_hdr), Paragraph("Acceptance Test", tbl_hdr)],
        [Paragraph("M1: Foundation", tbl_cell_bold), Paragraph("Xcode architecture, UI theme, FamilyControls auth", tbl_cell), Paragraph("FamilyControls", tbl_cell), Paragraph("Auth prompt & FaceID works on iPhone", tbl_cell)],
        [Paragraph("M2: Shield Engine", tbl_cell_bold), Paragraph("SetupView, AppPickerSheet, ChallengeView countdown", tbl_cell), Paragraph("ManagedSettings", tbl_cell), Paragraph("Target apps show native shield", tbl_cell)],
        [Paragraph("M3: Crypto Bypass", tbl_cell_bold), Paragraph("PinVault (0x5A) & TokenDecoder port, unlock modal", tbl_cell), Paragraph("CryptoKit", tbl_cell), Paragraph("Tokens decode PINs (5222, 2555, 3555)", tbl_cell)],
        [Paragraph("M4: Final Polish", tbl_cell_bold), Paragraph("CompletionView, auto shield teardown, ecosystem link", tbl_cell), Paragraph("DeviceActivity", tbl_cell), Paragraph("Clean 2m end-to-end lifecycle test", tbl_cell)]
    ]
    t_m = Table(m_raw, colWidths=[80, 190, 90, 170])
    t_m.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0F172A')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.HexColor('#F8FAFC'), colors.white]),
    ]))
    story.append(t_m)
    story.append(Spacer(1, 6))

    # Section 3
    story.append(Paragraph("3. Screen Architecture & User Flow (The 3 Core Screens)", h1_style))
    story.append(Paragraph("<b>Screen 1: Setup & Pre-Commitment (SetupView.swift)</b>", h2_style))
    story.append(Paragraph("• <b>Header Hero:</b> \"Analog Anchor Offline Challenge\" (Cyan #00E5FF on Obsidian #0A0E14). Subtitle: \"Choose intentional presence over subconscious screen consumption.\"<br/>"
                           "• <b>Philosophy Card:</b> Outbound link to main app ecosystem: <u>https://get-analog-anchor.com/</u>.<br/>"
                           "• <b>App Selection:</b> Button presents <code>FamilyActivityPicker</code>, persists tokens to App Group UserDefaults.<br/>"
                           "• <b>Duration Selection:</b> Matrix of 2m (Amber test mode), 12h, 18h, 36h, 72h. Immediately engages shield.", body_style))

    story.append(Paragraph("<b>Screen 2: Active Challenge & Countdown (ChallengeView.swift)</b>", h2_style))
    story.append(Paragraph("• <b>Circular Indicator:</b> Real-time circular progress ring with remaining time (<code>XXh YYm ZZs</code>) and percentage.<br/>"
                           "• <b>Shield Enforcement:</b> <code>ManagedSettingsStore().shield.applications = tokens</code> blocks selected apps immediately.<br/>"
                           "• <b>Emergency Bypass:</b> Modal accepts token (e.g. <code>AA-ANCHOR-5222...</code>). Verified via <code>TokenDecoder</code> and <code>PinVault</code>. Clears settings on match.", body_style))

    story.append(Paragraph("<b>Screen 3: Completion & Barakah (CompletionView.swift)</b>", h2_style))
    story.append(Paragraph("• <b>Automatic Trigger:</b> Fires upon timer expiration. Displays celebratory completion UI.<br/>"
                           "• <b>Shield Teardown:</b> Calls <code>ManagedSettingsStore().clearAllSettings()</code> automatically.<br/>"
                           "• <b>CTA:</b> Button \"Discover the Main Analog Anchor Ecosystem ↗\" links to <u>https://get-analog-anchor.com/</u>.", body_style))
    story.append(Spacer(1, 6))

    # Section 4
    story.append(Paragraph("4. Technical Specifications & File Structure", h1_style))
    file_tree_pdf = (
        "AnalogAnchorOfflineChallenge/<br/>"
        "├── App/<br/>"
        "│   ├── OfflineChallengeApp.swift   // App entry point + FamilyControls requestAuthorization<br/>"
        "│   └── AppTheme.swift              // Obsidian (#0A0E14), CyanGlow (#00E5FF), Amber (#FFB300)<br/>"
        "├── Features/<br/>"
        "│   ├── Setup/<br/>"
        "│   │   ├── SetupView.swift          // Pre-commitment card + Duration triggers<br/>"
        "│   │   └── AppPickerSheet.swift     // FamilyActivityPicker integration<br/>"
        "│   ├── Challenge/<br/>"
        "│   │   ├── ChallengeView.swift      // Live circular countdown + remaining timer<br/>"
        "│   │   └── EmergencyUnlockModal.swift // Token input field + TokenDecoder verification<br/>"
        "│   └── Completion/<br/>"
        "│       └── CompletionView.swift     // Celebration screen + Ecosystem link<br/>"
        "├── ScreenTime/<br/>"
        "│   ├── ScreenTimeManager.swift     // AuthorizationCenter + ManagedSettingsStore controller<br/>"
        "│   └── Model/<br/>"
        "│       └── ChallengeSession.swift  // Active session state, start time, target duration<br/>"
        "└── Crypto/<br/>"
        "    ├── PinVault.swift              // XOR salt 0x5A, verifying PINs 5222, 2555, 3555<br/>"
        "    └── TokenDecoder.swift          // NFKC Unicode normalization, cipher mapping, checksum"
    )
    t_tree = Table([[Paragraph(file_tree_pdf, code_style)]], colWidths=[530])
    t_tree.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(t_tree)
    story.append(Spacer(1, 6))

    # Section 5
    story.append(Paragraph("5. Verification & Acceptance Criteria", h1_style))
    story.append(Paragraph("1. <b>Build & Signing:</b> Must compile in Xcode 15/16 with zero errors and execute on physical iPhone (iOS 16+).<br/>"
                           "2. <b>Screen Time Authorization:</b> Invokes <code>AuthorizationCenter.shared.requestAuthorization(for: .individual)</code> with FaceID/Passcode prompt.<br/>"
                           "3. <b>Shielding Verification:</b> Blocked apps show native shield on tap during 2m test challenge; unblock automatically upon timer completion.<br/>"
                           "4. <b>Token Decoding:</b> Entering valid token generated from <code>decoy-tool.html</code> decodes PINs (5222, 2555, 3555) and unlocks session early.<br/>"
                           "5. <b>Persistence:</b> Session survives app swipe-kill and device reboot without releasing shields.", body_style))
    story.append(Spacer(1, 4))

    # Acceptance Checklist Table (PDF)
    chk_raw = [
        [Paragraph("Check", tbl_hdr), Paragraph("Acceptance Gate", tbl_hdr), Paragraph("Verification Requirement", tbl_hdr), Paragraph("Sign-off", tbl_hdr)],
        [Paragraph("[ &nbsp; ]", tbl_cell_bold), Paragraph("Gate 1: Build & Signing", tbl_cell_bold), Paragraph("Compiles in Xcode 15/16 with Swift 5.9+; installs on iOS 16+ device", tbl_cell), Paragraph("[ &nbsp; ] Pass &nbsp; [ &nbsp; ] Fail", tbl_cell)],
        [Paragraph("[ &nbsp; ]", tbl_cell_bold), Paragraph("Gate 2: Screen Time Auth", tbl_cell_bold), Paragraph("AuthorizationCenter individual prompt triggers and grants approval", tbl_cell), Paragraph("[ &nbsp; ] Pass &nbsp; [ &nbsp; ] Fail", tbl_cell)],
        [Paragraph("[ &nbsp; ]", tbl_cell_bold), Paragraph("Gate 3: Shielding", tbl_cell_bold), Paragraph("Selected apps in FamilyActivityPicker immediately show native shield", tbl_cell), Paragraph("[ &nbsp; ] Pass &nbsp; [ &nbsp; ] Fail", tbl_cell)],
        [Paragraph("[ &nbsp; ]", tbl_cell_bold), Paragraph("Gate 4: Crypto Bypass", tbl_cell_bold), Paragraph("Valid emergency token decodes PIN (5222, 2555, 3555) & clears shields", tbl_cell), Paragraph("[ &nbsp; ] Pass &nbsp; [ &nbsp; ] Fail", tbl_cell)],
        [Paragraph("[ &nbsp; ]", tbl_cell_bold), Paragraph("Gate 5: Auto Teardown", tbl_cell_bold), Paragraph("Reaching 00m 00s unshields apps automatically; renders CompletionView", tbl_cell), Paragraph("[ &nbsp; ] Pass &nbsp; [ &nbsp; ] Fail", tbl_cell)],
        [Paragraph("[ &nbsp; ]", tbl_cell_bold), Paragraph("Gate 6: Persistence", tbl_cell_bold), Paragraph("Shields survive app termination and device reboot; timer resumes", tbl_cell), Paragraph("[ &nbsp; ] Pass &nbsp; [ &nbsp; ] Fail", tbl_cell)]
    ]
    t_chk = Table(chk_raw, colWidths=[35, 125, 250, 120])
    t_chk.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0F172A')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 2.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 2.5),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.HexColor('#F8FAFC'), colors.white]),
    ]))
    story.append(t_chk)
    story.append(Spacer(1, 8))

    # Sign-off Block
    sign_raw = [
        [Paragraph("<b>Client / IP Owner: Analog Anchor</b>", tbl_cell_bold), Paragraph("<b>Lead iOS Engineer / Contractor:</b>", tbl_cell_bold)],
        [Paragraph("Signature: __________________________<br/>Date: ____ / ____ / 2026", tbl_cell), Paragraph("Signature: __________________________<br/>Date: ____ / ____ / 2026", tbl_cell)]
    ]
    t_sign = Table(sign_raw, colWidths=[265, 265])
    t_sign.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(t_sign)

    doc.build(story)
    print(f"PDF successfully generated at: {filepath}")

if __name__ == "__main__":
    desktop_dir = r"C:\Users\DELL\Desktop"
    
    docx_file1 = os.path.join(desktop_dir, "Statement_of_Work_Analog_Anchor_Offline_Challenge_iOS.docx")
    docx_file2 = os.path.join(desktop_dir, "Analog_Anchor_Offline_Challenge_SOW_and_Tech_Spec.docx")
    pdf_file = os.path.join(desktop_dir, "Statement_of_Work_Analog_Anchor_Offline_Challenge_iOS.pdf")
    
    generate_docx(docx_file1)
    generate_docx(docx_file2)
    generate_pdf(pdf_file)
    print("All documents generated successfully on Desktop!")
