from fpdf import FPDF
import os

class TickExpPDF(FPDF):
    def header(self):
        self.set_fill_color(156, 72, 234) # PrimaryPurple
        self.rect(0, 0, 210, 40, 'F')
        self.set_font('Helvetica', 'B', 24)
        self.set_text_color(255, 255, 255)
        self.cell(0, 20, 'TickExp Project Specification', ln=True, align='C')
        self.set_font('Helvetica', 'I', 10)
        self.cell(0, 10, 'The Golden Build - v1.1', ln=True, align='C')
        self.ln(10)

    def footer(self):
        self.set_y(-15)
        self.set_font('Helvetica', 'I', 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f'Page {self.page_no()}', align='C')

def generate_pdf():
    pdf = TickExpPDF()
    pdf.add_page()
    pdf.set_auto_page_break(auto=True, margin=15)
    
    with open('TickExp_Project_Summary.md', 'r', encoding='utf-8') as f:
        lines = f.readlines()

    pdf.set_text_color(0, 0, 0)
    for line in lines:
        # Strip non-ASCII characters (emojis) to avoid encoding errors
        line = "".join(char for char in line if ord(char) < 128).strip()
        
        if not line:
            pdf.ln(5)
            continue
            
        if line.startswith('# '):
            continue # Already in header
        elif line.startswith('### '):
            pdf.set_font('Helvetica', 'B', 16)
            pdf.set_text_color(156, 72, 234)
            pdf.cell(0, 10, line.replace('### ', ''), ln=True)
            pdf.set_text_color(0, 0, 0)
        elif line.startswith('* '):
            pdf.set_font('Helvetica', '', 11)
            pdf.set_x(20)
            pdf.multi_cell(0, 7, f'- {line.replace("* ", "")}')
        elif line.startswith('---'):
            pdf.line(10, pdf.get_y(), 200, pdf.get_y())
            pdf.ln(5)
        else:
            if '**' in line:
                # Basic bold handling for the whole line if it contains **
                pdf.set_font('Helvetica', 'B', 11)
                pdf.multi_cell(0, 7, line.replace('**', ''))
            else:
                pdf.set_font('Helvetica', '', 11)
                pdf.multi_cell(0, 7, line)
        
    pdf.output('TickExp_Technical_Specification.pdf')
    print("PDF Generated: TickExp_Technical_Specification.pdf")

if __name__ == "__main__":
    generate_pdf()
