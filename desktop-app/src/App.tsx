import React, { useState, useEffect } from 'react'
import { invoke } from '@tauri-apps/api/core'
import './App.css'

interface Device {
  id: string
  name: string
  ip: string
  status: 'online' | 'offline'
}

interface TransferProgress {
  filename: string
  progress: number
  speed: string
  eta: string
}

function App() {
  const [devices, setDevices] = useState<Device[]>([])
  const [selectedFiles, setSelectedFiles] = useState<File[]>([])
  const [transferProgress, setTransferProgress] = useState<TransferProgress | null>(null)
  const [isScanning, setIsScanning] = useState(false)

  useEffect(() => {
    startDeviceDiscovery()
  }, [])

  const startDeviceDiscovery = async () => {
    setIsScanning(true)
    try {
      const discoveredDevices = await invoke<Device[]>('discover_devices')
      setDevices(discoveredDevices)
    } catch (error) {
      console.error('Failed to discover devices:', error)
    } finally {
      setIsScanning(false)
    }
  }

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || [])
    setSelectedFiles(files)
  }

  const handleFileDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    const files = Array.from(event.dataTransfer.files)
    setSelectedFiles(files)
  }

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
  }

  const transferFiles = async (targetDevice: Device) => {
    if (selectedFiles.length === 0) {
      alert('Please select files to transfer')
      return
    }

    try {
      for (const file of selectedFiles) {
        setTransferProgress({
          filename: file.name,
          progress: 0,
          speed: '0 MB/s',
          eta: 'Calculating...'
        })

        await invoke('transfer_file', {
          filePath: file.name,
          targetDeviceId: targetDevice.id,
          targetIp: targetDevice.ip
        })
      }
      
      setTransferProgress(null)
      setSelectedFiles([])
      alert('Files transferred successfully!')
    } catch (error) {
      console.error('Transfer failed:', error)
      alert('Transfer failed: ' + error)
      setTransferProgress(null)
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Enterprise File Transfer</h1>
        <p>Secure, fast file sharing across devices</p>
      </header>

      <main className="app-main">
        <section className="file-selection">
          <h2>Select Files</h2>
          <div 
            className="drop-zone"
            onDrop={handleFileDrop}
            onDragOver={handleDragOver}
          >
            <input
              type="file"
              multiple
              onChange={handleFileSelect}
              className="file-input"
              id="file-input"
            />
            <label htmlFor="file-input" className="file-input-label">
              {selectedFiles.length > 0 
                ? `${selectedFiles.length} file(s) selected`
                : 'Click to select files or drag & drop here'
              }
            </label>
          </div>
          
          {selectedFiles.length > 0 && (
            <div className="selected-files">
              <h3>Selected Files:</h3>
              <ul>
                {selectedFiles.map((file, index) => (
                  <li key={index}>
                    {file.name} ({(file.size / 1024 / 1024).toFixed(2)} MB)
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>

        <section className="device-discovery">
          <div className="section-header">
            <h2>Available Devices</h2>
            <button 
              onClick={startDeviceDiscovery}
              disabled={isScanning}
              className="refresh-btn"
            >
              {isScanning ? 'Scanning...' : 'Refresh'}
            </button>
          </div>
          
          <div className="devices-grid">
            {devices.length === 0 ? (
              <p className="no-devices">
                {isScanning ? 'Scanning for devices...' : 'No devices found'}
              </p>
            ) : (
              devices.map((device) => (
                <div key={device.id} className="device-card">
                  <div className="device-info">
                    <h3>{device.name}</h3>
                    <p>{device.ip}</p>
                    <span className={`status ${device.status}`}>
                      {device.status}
                    </span>
                  </div>
                  <button
                    onClick={() => transferFiles(device)}
                    disabled={selectedFiles.length === 0 || device.status === 'offline'}
                    className="transfer-btn"
                  >
                    Send Files
                  </button>
                </div>
              ))
            )}
          </div>
        </section>

        {transferProgress && (
          <section className="transfer-progress">
            <h2>Transfer Progress</h2>
            <div className="progress-info">
              <p><strong>File:</strong> {transferProgress.filename}</p>
              <div className="progress-bar">
                <div 
                  className="progress-fill"
                  style={{ width: `${transferProgress.progress}%` }}
                ></div>
              </div>
              <div className="progress-stats">
                <span>{transferProgress.progress.toFixed(1)}%</span>
                <span>{transferProgress.speed}</span>
                <span>ETA: {transferProgress.eta}</span>
              </div>
            </div>
          </section>
        )}
      </main>
    </div>
  )
}

export default App
