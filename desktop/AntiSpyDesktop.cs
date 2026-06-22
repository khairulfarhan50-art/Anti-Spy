using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Windows.Forms;
using System.Runtime.InteropServices;

namespace AntiSpyDesktop
{
    public class OverlayForm : Form
    {
        [DllImport("user32.dll", EntryPoint = "GetWindowLong")]
        private static extern int GetWindowLong(IntPtr hWnd, int nIndex);

        [DllImport("user32.dll", EntryPoint = "SetWindowLong")]
        private static extern int SetWindowLong(IntPtr hWnd, int nIndex, int dwNewLong);

        private const int GWL_EXSTYLE = -20;
        private const int WS_EX_TRANSPARENT = 0x20;
        private const int WS_EX_LAYERED = 0x80000;

        private float overlayOpacity = 0.5f;
        private string patternType = "lines";
        private Bitmap noiseTexture = null;
        private TextureBrush noiseBrush = null;
        private readonly Color transparencyKeyColor = Color.Fuchsia;

        public OverlayForm()
        {
            // Configure form properties
            this.FormBorderStyle = FormBorderStyle.None;
            this.ShowInTaskbar = false;
            this.TopMost = true;
            this.StartPosition = FormStartPosition.Manual;

            // Cover all active monitors
            Rectangle virtualScreen = SystemInformation.VirtualScreen;
            this.Left = virtualScreen.Left;
            this.Top = virtualScreen.Top;
            this.Width = virtualScreen.Width;
            this.Height = virtualScreen.Height;

            // Transparency settings
            this.BackColor = transparencyKeyColor;
            this.TransparencyKey = transparencyKeyColor;
            this.DoubleBuffered = true;

            // Initial overlay properties
            this.Opacity = overlayOpacity;

            GenerateNoiseTexture();
        }

        protected override void OnLoad(EventArgs e)
        {
            base.OnLoad(e);
            // Apply click-through (WS_EX_TRANSPARENT) and layered (WS_EX_LAYERED) window styles
            int initialStyle = GetWindowLong(this.Handle, GWL_EXSTYLE);
            SetWindowLong(this.Handle, GWL_EXSTYLE, initialStyle | WS_EX_TRANSPARENT | WS_EX_LAYERED);
        }

        public void UpdateSettings(float opacity, string pattern)
        {
            this.overlayOpacity = opacity;
            this.patternType = pattern.ToLower();
            this.Opacity = overlayOpacity;
            this.Invalidate(); // Force repaint
        }

        private void GenerateNoiseTexture()
        {
            if (noiseTexture != null)
            {
                noiseBrush.Dispose();
                noiseTexture.Dispose();
            }

            // Create a small 128x128 tiled noise bitmap
            noiseTexture = new Bitmap(128, 128);
            Random rand = new Random();
            for (int x = 0; x < noiseTexture.Width; x++)
            {
                for (int y = 0; y < noiseTexture.Height; y++)
                {
                    if (rand.Next(2) == 0)
                    {
                        noiseTexture.SetPixel(x, y, Color.Black);
                    }
                    else
                    {
                        noiseTexture.SetPixel(x, y, transparencyKeyColor);
                    }
                }
            }
            noiseBrush = new TextureBrush(noiseTexture);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            base.OnPaint(e);
            Graphics g = e.Graphics;

            // Clear background with transparency key
            g.Clear(transparencyKeyColor);

            switch (patternType)
            {
                case "dim":
                    // Paint entire screen black
                    using (SolidBrush brush = new SolidBrush(Color.Black))
                    {
                        g.FillRectangle(brush, 0, 0, this.Width, this.Height);
                    }
                    break;

                case "lines":
                    // Draw vertical stripes
                    using (Pen pen = new Pen(Color.Black, 2.5f))
                    {
                        float pitch = 7f;
                        for (float x = 0; x < this.Width; x += pitch)
                        {
                            g.DrawLine(pen, x, 0, x, this.Height);
                        }
                    }
                    break;

                case "crosshatch":
                    // Draw diagonal mesh
                    using (Pen pen = new Pen(Color.Black, 2f))
                    {
                        float pitch = 14f;
                        float limit = Math.Max(this.Width, this.Height);
                        for (float offset = -limit; offset < limit; offset += pitch)
                        {
                            // Diagonal /
                            g.DrawLine(pen, offset, 0f, offset + (float)this.Height, (float)this.Height);
                            // Diagonal \
                            g.DrawLine(pen, offset + (float)this.Height, 0f, offset, (float)this.Height);
                        }
                    }
                    break;

                case "noise":
                    // Draw noise texture
                    if (noiseBrush != null)
                    {
                        g.FillRectangle(noiseBrush, 0, 0, this.Width, this.Height);
                    }
                    break;
            }
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                if (noiseBrush != null) noiseBrush.Dispose();
                if (noiseTexture != null) noiseTexture.Dispose();
            }
            base.Dispose(disposing);
        }
    }

    public class AntiSpyApplicationContext : ApplicationContext
    {
        private NotifyIcon trayIcon;
        private OverlayForm overlayForm;
        private bool isEnabled = true;
        private float currentOpacity = 0.5f;
        private string currentPattern = "lines";

        private MenuItem menuEnable;
        private MenuItem menuPattern;
        private MenuItem menuOpacity;

        public AntiSpyApplicationContext()
        {
            // Initialize overlay form
            overlayForm = new OverlayForm();
            overlayForm.UpdateSettings(currentOpacity, currentPattern);
            overlayForm.Show();

            // Setup tray icon and context menu
            InitializeTray();
        }

        private void InitializeTray()
        {
            ContextMenu trayMenu = new ContextMenu();

            // Enable/Disable toggle
            menuEnable = new MenuItem("Aktifkan Filter", ToggleFilter);
            menuEnable.Checked = isEnabled;
            trayMenu.MenuItems.Add(menuEnable);

            trayMenu.MenuItems.Add("-"); // Separator

            // Pattern submenu
            menuPattern = new MenuItem("Pola Filter");
            menuPattern.MenuItems.Add(new MenuItem("Hanya Redup (Dim)", SetPattern) { Tag = "dim" });
            menuPattern.MenuItems.Add(new MenuItem("Garis Vertikal (Lines)", SetPattern) { Tag = "lines" });
            menuPattern.MenuItems.Add(new MenuItem("Silang Diagonal (Crosshatch)", SetPattern) { Tag = "crosshatch" });
            menuPattern.MenuItems.Add(new MenuItem("Bintik Noise (Noise)", SetPattern) { Tag = "noise" });
            UpdatePatternMenuChecks();
            trayMenu.MenuItems.Add(menuPattern);

            // Opacity submenu
            menuOpacity = new MenuItem("Tingkat Kegelapan");
            for (int i = 1; i <= 9; i++)
            {
                float val = i / 10f;
                menuOpacity.MenuItems.Add(new MenuItem((i * 10) + "%", SetOpacity) { Tag = val });
            }
            UpdateOpacityMenuChecks();
            trayMenu.MenuItems.Add(menuOpacity);

            trayMenu.MenuItems.Add("-"); // Separator

            // Exit application
            trayMenu.MenuItems.Add("Keluar", ExitApp);

            // Create notification tray icon
            trayIcon = new NotifyIcon();
            trayIcon.Text = "Anti-Spy Display Filter";
            trayIcon.Icon = SystemIcons.Shield; // Use system shield icon
            trayIcon.ContextMenu = trayMenu;
            trayIcon.Visible = true;

            // Quick double click toggles the overlay
            trayIcon.DoubleClick += (s, e) => ToggleFilter(s, e);
        }

        private void ToggleFilter(object sender, EventArgs e)
        {
            isEnabled = !isEnabled;
            menuEnable.Checked = isEnabled;
            if (isEnabled)
            {
                overlayForm.Show();
            }
            else
            {
                overlayForm.Hide();
            }
        }

        private void SetPattern(object sender, EventArgs e)
        {
            MenuItem item = sender as MenuItem;
            if (item != null && item.Tag != null)
            {
                currentPattern = item.Tag.ToString();
                UpdatePatternMenuChecks();
                if (isEnabled)
                {
                    overlayForm.UpdateSettings(currentOpacity, currentPattern);
                }
            }
        }

        private void UpdatePatternMenuChecks()
        {
            foreach (MenuItem item in menuPattern.MenuItems)
            {
                if (item.Tag != null)
                {
                    item.Checked = (item.Tag.ToString() == currentPattern);
                }
            }
        }

        private void SetOpacity(object sender, EventArgs e)
        {
            MenuItem item = sender as MenuItem;
            if (item != null && item.Tag != null)
            {
                currentOpacity = (float)item.Tag;
                UpdateOpacityMenuChecks();
                if (isEnabled)
                {
                    overlayForm.UpdateSettings(currentOpacity, currentPattern);
                }
            }
        }

        private void UpdateOpacityMenuChecks()
        {
            foreach (MenuItem item in menuOpacity.MenuItems)
            {
                if (item.Tag != null)
                {
                    float val = (float)item.Tag;
                    item.Checked = (Math.Abs(val - currentOpacity) < 0.01f);
                }
            }
        }

        private void ExitApp(object sender, EventArgs e)
        {
            trayIcon.Visible = false;
            trayIcon.Dispose();
            overlayForm.Close();
            Application.Exit();
        }
    }

    public static class Program
    {
        [STAThread]
        public static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new AntiSpyApplicationContext());
        }
    }
}
