package com.strath.finalpr0ject.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.strath.finalpr0ject.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Child3DDashboardScreen(
    childId: String,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ChildCoral.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChildCare, contentDescription = null, tint = ChildCoral)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Akiru's Growth Journey", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            Text("ID: $childId • Age: 18 months", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3D Interactive Milestone & Growth Canvas Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Embed WebGL / Three.js 3D Interactive Viewer via AndroidView WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                webViewClient = WebViewClient()
                                val threeJsHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                                  <style>
                                    body { margin: 0; overflow: hidden; background: radial-gradient(circle, #1a2a3a 0%, #0b1118 100%); }
                                    #label { position: absolute; bottom: 12px; width: 100%; text-align: center; color: #73f8e7; font-family: sans-serif; font-size: 11px; font-weight: bold; }
                                  </style>
                                  <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
                                </head>
                                <body>
                                  <div id="label">✨ Interactive 3D Pediatric Growth Model • Drag to Rotate</div>
                                  <script>
                                    const scene = new THREE.Scene();
                                    const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 1000);
                                    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
                                    renderer.setSize(window.innerWidth, window.innerHeight);
                                    document.body.appendChild(renderer.domElement);

                                    // Lights
                                    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
                                    scene.add(ambientLight);
                                    const dirLight = new THREE.DirectionalLight(0xffd54f, 1.2);
                                    dirLight.position.set(5, 10, 7);
                                    scene.add(dirLight);

                                    // 3D Pediatric Growth Figure (Stylized Child Model)
                                    const childGroup = new THREE.Group();

                                    // Head
                                    const headGeo = new THREE.SphereGeometry(0.85, 32, 32);
                                    const skinMat = new THREE.MeshStandardMaterial({ color: 0x8d5524, roughness: 0.4 });
                                    const head = new THREE.Mesh(headGeo, skinMat);
                                    head.position.y = 2.1;
                                    childGroup.add(head);

                                    // Torso
                                    const torsoGeo = new THREE.CylinderGeometry(0.55, 0.65, 1.4, 32);
                                    const shirtMat = new THREE.MeshStandardMaterial({ color: 0x006a60, roughness: 0.5 });
                                    const torso = new THREE.Mesh(torsoGeo, shirtMat);
                                    torso.position.y = 0.8;
                                    childGroup.add(torso);

                                    // 3D Milestone Indicator Rings (Floating)
                                    const ringGeo = new THREE.TorusGeometry(1.2, 0.04, 16, 100);
                                    const ringMat = new THREE.MeshBasicMaterial({ color: 0x73f8e7 });
                                    const motorRing = new THREE.Mesh(ringGeo, ringMat);
                                    motorRing.rotation.x = Math.PI / 2;
                                    motorRing.position.y = 0.8;
                                    childGroup.add(motorRing);

                                    // Growth Height Gauge (Glowing vertical bar)
                                    const barGeo = new THREE.CylinderGeometry(0.04, 0.04, 3.5, 16);
                                    const barMat = new THREE.MeshBasicMaterial({ color: 0xff6f61 });
                                    const growthBar = new THREE.Mesh(barGeo, barMat);
                                    growthBar.position.set(-1.2, 1.2, 0);
                                    childGroup.add(growthBar);

                                    scene.add(childGroup);
                                    camera.position.set(0, 1.5, 5);

                                    // Touch / Mouse Rotation
                                    let isDragging = false;
                                    let prevX = 0;
                                    window.addEventListener('pointerdown', (e) => { isDragging = true; prevX = e.clientX; });
                                    window.addEventListener('pointermove', (e) => {
                                      if (isDragging) {
                                        const dx = e.clientX - prevX;
                                        childGroup.rotation.y += dx * 0.01;
                                        prevX = e.clientX;
                                      }
                                    });
                                    window.addEventListener('pointerup', () => { isDragging = false; });

                                    function animate() {
                                      requestAnimationFrame(animate);
                                      if (!isDragging) {
                                        childGroup.rotation.y += 0.008;
                                      }
                                      motorRing.rotation.z += 0.01;
                                      renderer.render(scene, camera);
                                    }
                                    animate();
                                  </script>
                                </body>
                                </html>
                                """.trimIndent()
                                loadDataWithBaseURL(null, threeJsHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 3D Overlay Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            "3D Bio-Milestone Model",
                            color = TealPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Milestone Progress Summary
            Text("Developmental Milestones (ASQ-3)", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Motor
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gross Motor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("85%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                    }
                }

                // Cognitive
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF0277BD))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Problem Solving", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("80%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0277BD))
                    }
                }

                // Language
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF512DA8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Language", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("75%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF512DA8))
                    }
                }
            }

            // Caregiver Nutrition & Action Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = ChildCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nutrition Plan for Akiru", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        "• Introduce camel milk and mashed eggs to boost zinc and protein intake.\n• Continue age-appropriate play and responsive feeding.\n• Next screening scheduled with CHW Maria Akiru in 14 days.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }
            }

            // Assigned CHW Contact Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TealPrimaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Assigned Community Health Worker", fontSize = 11.sp, color = TealOnPrimaryContainer)
                        Text("Maria Akiru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealOnPrimaryContainer)
                        Text("Kakuma West Health Sub-Center", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    FilledTonalButton(
                        onClick = { /* Call or message CHW */ },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun Child3DDashboardScreenPreview() {
    Child3DDashboardScreen(
        childId = "TK-1042",
        onSignOut = {}
    )
}
